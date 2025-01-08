/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple5;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.python.PythonTransforms;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;
import com.hazelcast.platform.demos.retail.clickstream.PredictionKey;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Run the Gaussian version of ML model against input stream
 * </p>
 * <p>TODO: Almost identical to {@link DecisionTreePrediction}.
 * </p>
 */
@Slf4j
public class GaussianPrediction {
    private static final String ALGORITHM =
            GaussianPrediction.class.getSimpleName().replaceFirst("Prediction", "");
    private static final int EXPECTED_LENGTH_OF_5 = 5;
    private static final int FIRST_KEY = 0;
    private static final int SECOND_PUBLISH_TIMESTAMP = 1;
    private static final int THIRD_INGEST_TIMESTAMP = 2;
    private static final int FOURTH_MODEL_VERSION = 3;
    private static final int FIFTH_PREDICTION = 4;
    private static final String PYTHON_MODULE = "gaussian_predict";
    private static final String PYTHON_HANDLER_FN = "predict";
    private static final long FIVE = 5L;

    public static Pipeline buildPipeline(String graphiteHost, ClassLoader classLoader) {
        Pipeline pipeline = Pipeline.create();

        try {
            StreamStage<String> input =
                    pipeline
                    .readFrom(Sources.<String, Tuple3<Long, Long, String>>mapJournal(
                            MyConstants.IMAP_NAME_CHECKOUT,
                            JournalInitialPosition.START_FROM_OLDEST)).withoutTimestamps()
                    .map(Entry::getKey);

            // Format to this style: "data,aaaa,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1"
            StreamStage<String> inputFormatted =
                    input
                    .mapUsingIMap(MyConstants.IMAP_NAME_DIGITAL_TWIN,
                            FunctionEx.identity(),
                            (String key, Tuple3<Long, Long, String> digitalTwin) -> {
                                if (digitalTwin == null) {
                                    return null;
                                }
                                String arrPrint = Arrays.toString(MyUtils.digitalTwinCsvToBinary(null, digitalTwin.f2(), false));
                                return "data," + key + "," + digitalTwin.f0() + ","
                                        + digitalTwin.f1() + "," + arrPrint.substring(1, arrPrint.length() - 1);
                            });

            // Python prediction
            StreamStage<String> pythonOutput =
                inputFormatted
                .apply(PythonTransforms.mapUsingPython(
                            MyUtils.getPythonServiceConfig(PYTHON_MODULE, PYTHON_HANDLER_FN, classLoader)))
                                .setLocalParallelism(1)
                                .setName(PYTHON_MODULE);

            // Format for saving to an IMap, including prediction time
            StreamStage<Entry<PredictionKey, Tuple5<String, Long, Long, Long, Integer>>> outputFormatted =
                pythonOutput
                .map(GaussianPrediction::format);

            outputFormatted.writeTo(Sinks.map(MyConstants.IMAP_NAME_PREDICTION));

            // Diagnostic loggers
            //MyUtils.addExponentialLogger(inputFormatted, "inputFormatted", FIVE);
            MyUtils.addExponentialLogger(pythonOutput, "pythonOutput", FIVE);
            //MyUtils.addExponentialLogger(outputFormatted, "outputFormatted", FIVE);
        } catch (Exception e) {
            log.error("buildPipeline()", e);
            return null;
        }

        return pipeline;
    }

    /**
     * <p>Turn Python prediction output into a Map entry.
     * </p>
     *
     * @param str
     * @return
     */
    static Entry<PredictionKey, Tuple5<String, Long, Long, Long, Integer>> format(String str) {
        String[] tokens = str.split(",");

        if (tokens.length != EXPECTED_LENGTH_OF_5) {
            log.error("format('{}'), {}!={} tokens", str, tokens.length, EXPECTED_LENGTH_OF_5);
            return null;
        }

        List<String> tokenList = Arrays.asList(tokens);
        String key = tokenList.get(FIRST_KEY);
        Long publishTimestamp = Long.parseLong(tokenList.get(SECOND_PUBLISH_TIMESTAMP));
        Long ingestTimestamp = Long.parseLong(tokenList.get(THIRD_INGEST_TIMESTAMP));
        Long predictionTimestamp = System.currentTimeMillis();
        String version = tokenList.get(FOURTH_MODEL_VERSION);
        if (version.endsWith("Z")) {
            version = version.substring(0, version.length() - 1);
        }
        Integer prediction = Integer.parseInt(tokenList.get(FIFTH_PREDICTION));

        PredictionKey predictionKey = new PredictionKey();
        predictionKey.setAlgorithm(ALGORITHM);
        predictionKey.setKey(key);

        Tuple5<String, Long, Long, Long, Integer> predictionValue
            = Tuple5.<String, Long, Long, Long, Integer>tuple5(
                    version, publishTimestamp, ingestTimestamp, predictionTimestamp, prediction);

        Entry<PredictionKey, Tuple5<String, Long, Long, Long, Integer>> entry
            = new SimpleImmutableEntry<>(predictionKey, predictionValue);

        return entry;
    }
}
