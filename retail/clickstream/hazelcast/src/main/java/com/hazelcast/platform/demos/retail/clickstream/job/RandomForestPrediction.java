/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.util.Map.Entry;

import com.hazelcast.function.Functions;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Edge;
import com.hazelcast.jet.core.EventTimePolicy;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.processor.Processors;
import com.hazelcast.jet.core.processor.SinkProcessors;
import com.hazelcast.jet.core.processor.SourceProcessors;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.python.DagPythonMetaSupplier;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Continuously stream data into Random Forest model to predict
 * action, and measure latency to produce prediction.
 * </p>
 * <p>The input will usually be data, coming from the clickstream
 * and triggered by visiting the checkout page. It may also be
 * a model from periodic retraining.
 * </p>
 * <p>Include a timestamp in the output, so can measure end-to-end
 * latency
 * </p>
 */
@Slf4j
public class RandomForestPrediction {
    private static final String ALGORITHM =
            RandomForestPrediction.class.getSimpleName().replaceFirst("Prediction", "");
    private static final String PYTHON_MODULE = "randomforest_predict";
    private static final String PYTHON_HANDLER_FN = "predict";
    private static final int SIDE_OUTPUT = 1;
    private static final long TEN = 10L;

    /**
     * <p>Build as a {@link DAG} not {@link Pipeline} as
     * distributing the model needs to be a broadcast node.
     * </p>
     * <p><b>Note:</b> Pipeline is easier to understand, but if
     * you need a DAG, here's how it is done.
     * </p>
     */
    public static DAG buildDAG() {
        DAG dag = new DAG();

        try {
            // Build the left input leg, model, returns the last stage
            Vertex inputLeftCsv = buildLeftInput(dag);

            // Build the right input leg, data, returns the last stage
            Vertex inputRightCsv = buildInputRight(dag);

            // Interleave left left, model, with right leg, data
            Vertex myMerge = buildMerge(dag, inputLeftCsv, inputRightCsv);

            // Pass merged model and data into Python
            Vertex pythonOutput = buildPythonOutput(dag, myMerge);

            // Turn into a map entry
            Vertex outputFormatted = buildOutputFormatted(dag, pythonOutput);

            // And save
            Vertex mapSink = dag.newVertex(MyConstants.IMAP_NAME_PREDICTION + "!sink",
                    SinkProcessors.writeMapP(MyConstants.IMAP_NAME_PREDICTION));
            dag.edge(Edge.between(outputFormatted, mapSink));

            // Optional diagnostic logger
            MyUtils.addExponentialLogger(dag, outputFormatted, TEN, SIDE_OUTPUT);
        } catch (Exception e) {
            log.error("buildDAG()", e);
            return null;
        }

        return dag;
    }

    /**
     * <p>Build the input leg for models, the stream of models.
     * Format to this style:
     * <pre>
     *  "model,RandomForest-1631114729714,gANjc2tsZWFybi5lbnNlbWJsZS5fZm9yZXN"
     * </pre>
     * </p>
     * <p>Prefix "data", then model name, then the model.
     * </p>
     *
     * @param dag
     * @return
     */
    private static Vertex buildLeftInput(DAG dag) {
        // A continuous stream, updating as new models are published, which is infrequent
        Vertex inputLeft = dag.newVertex(MyConstants.IMAP_NAME_MODEL_SELECTION + "!journal",
                SourceProcessors.streamMapP(MyConstants.IMAP_NAME_MODEL_SELECTION,
                        JournalInitialPosition.START_FROM_OLDEST,
                        EventTimePolicy.noEventTime()));

        // Only want updates to RandomForest models
        Vertex inputLeftFilter = dag.newVertex(MyConstants.IMAP_NAME_MODEL_SELECTION + "!filter",
                Processors.filterP((Entry<?, ?> entry) -> entry.getKey().toString().equals(ALGORITHM)));

        // Get the model key to lookup from Cassandra
        Vertex inputLeftKey = dag.newVertex(MyConstants.IMAP_NAME_MODEL_SELECTION + "!key",
                Processors.mapP(Functions.entryValue()));

        // Get from map, via MapLoader
        Vertex inputLeftEntry = dag.newVertex(MyConstants.IMAP_NAME_MODEL_SELECTION + "!entry",
                new ModelMetaSupplier());

        // Prepare CSV input for Python
        Vertex inputLeftCsv = dag.newVertex(MyConstants.IMAP_NAME_MODEL_SELECTION + "!csv",
                Processors.mapP((Entry<String, String> entry) -> {
                    return "model," + entry.getKey() + "," + entry.getValue();
                }));

        dag.edge(Edge.between(inputLeft, inputLeftFilter));
        dag.edge(Edge.between(inputLeftFilter, inputLeftKey));
        dag.edge(Edge.between(inputLeftKey, inputLeftEntry));
        dag.edge(Edge.between(inputLeftEntry, inputLeftCsv));

        return inputLeftCsv;
    }

    /**
     * <p>Build the input leg for data, the stream of user actions.
     * Format to this style:
     * <pre>
     *  "data,aaaa,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1"
     * </pre>
     * </p>
     * <p>Prefix "data", then key, publish time, ingest time, and observed actions.
     * </p>
     *
     * @param dag
     * @return
     */
    private static Vertex buildInputRight(DAG dag) {
        // A continuous stream, when the customer goes to the checkout is the trigger to prediction
        Vertex inputRight = dag.newVertex(MyConstants.IMAP_NAME_CHECKOUT + "!journal",
                SourceProcessors.streamMapP(MyConstants.IMAP_NAME_CHECKOUT,
                        JournalInitialPosition.START_FROM_OLDEST,
                        EventTimePolicy.noEventTime()));

        // From the checkout, we only need the key to look up the stored data
        Vertex inputRightKey = dag.newVertex(MyConstants.IMAP_NAME_CHECKOUT + "!key",
                Processors.mapP(Functions.entryKey()));

        // Get from map, as CSV
        Vertex inputRightCsv = dag.newVertex(MyConstants.IMAP_NAME_DIGITAL_TWIN + "!csv",
                new DigitalTwinMetaSupplier());

        dag.edge(Edge.between(inputRight, inputRightKey));
        dag.edge(Edge.between(inputRightKey, inputRightCsv));

        return inputRightCsv;
    }

    /**
     * <p>Merge the left side (new models) with the right side (data),
     * using a custom merge module that can do logging.
     * </p>
     * <p>Note the model side is a broadcast join, models need send to all
     * Python runners, not partitioned.
     * </p>
     *
     * @param dag
     * @param inputLeftModel
     * @param inputRightKey
     * @return
     */
    private static Vertex buildMerge(DAG dag, Vertex inputLeftCsv, Vertex inputRightCsv) {
        Vertex myMerge = dag.newVertex(MyMergeProcessor.class.getSimpleName(), MyMergeProcessor::new);

        dag.edge(Edge.from(inputLeftCsv, 0).to(myMerge, 0).broadcast().distributed());
        dag.edge(Edge.from(inputRightCsv, 0).to(myMerge, 1));

        return myMerge;
    }

    /**
     * <p>Run Python
     * </p>
     *
     * @param dag
     * @param myMerge
     * @return
     */
    private static Vertex buildPythonOutput(DAG dag, Vertex myMerge) throws Exception {
        Vertex pythonOutput = dag.newVertex(PYTHON_MODULE,
                new DagPythonMetaSupplier(PYTHON_MODULE, PYTHON_HANDLER_FN));

        dag.edge(Edge.between(myMerge, pythonOutput));

        return pythonOutput;
    }

    /**
     * <p>Reformat Python's output string to a map entry. Do this as
     * a separate stage so could log.
     * </p>
     *
     * @param dag
     * @param pythonOutput
     * @return
     */
    private static Vertex buildOutputFormatted(DAG dag, Vertex pythonOutput) {
        Vertex outputFormatted = dag.newVertex(RandomForestPredictionProcessor.class.getSimpleName(),
                () -> new RandomForestPredictionProcessor(ALGORITHM));

        dag.edge(Edge.between(pythonOutput, outputFormatted));

        return outputFormatted;
    }

}
