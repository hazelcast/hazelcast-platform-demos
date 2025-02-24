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

import java.util.Arrays;
import java.util.Map.Entry;

import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.python.PythonTransforms;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Pass a block of input through a model and compare predictions against actuality.
 * </p>
 * <p>This begins the same as {@code RandomForestPrediction} but has actual output
 * to compare against to assess if the prediction was correct or not.
 * </p>
 * <p>Accuracy is assessed by looking in the "{@code ordered}" map to see if the
 * customer bought. No entry means buy didn't happen, customer abandoned. Note
 * this means the validation has to wait {@link MyConstants.VALIDATION_GRACE_PERIOD}
 * to give the human time to press the buy button.
 * </p>
 * <p><b>NOTE</b> This evaluation uses "checkout" as input, the presence of "order"
 * is matched against the prediction, as the prediction is whether "order" will or
 * won't appear. We want to know if positive predictions and negative predictions
 * are correct.
 * </p>
 * <p>But see also {@link StatisticsAccuracyByOrder} which does a different
 * variation of the calculation.
 * </p>
 */
@Slf4j
public class RandomForestValidation {
    private static final String PYTHON_MODULE = "randomforest_predict";
    private static final String PYTHON_HANDLER_FN = "predict";
    //private static final long FIVE = 5L;
    private static final int EXPECTED_LENGTH_OF_5 = 5;
    private static final int LOCATION_OF_KEY_FIELD_0 = 0;
    private static final int LOCATION_OF_PREDICTION_FIELD_4 = 4;

    /**
     * <p>Produce "{@code <String, Double>}" with the double being the
     * accuracy.
     * </p>
     */
    public static Pipeline buildPipeline(long start, long end, String modelName) {
        Pipeline pipeline = Pipeline.create();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            // Left leg, first handled, is the model to use
            // Format to this style: "model,RandomForest-1631114729714,gANjc2tsZWFybi5lbnNlbWJsZS5fZm9yZXN"
            BatchStage<String> inputLeftFormatted =
                    pipeline
                    .readFrom(RandomForestValidation.mapValue(modelName))
                    .map(model -> "model," + modelName + "," + model);

            // Right leg, is a block of input to predict
            BatchStage<Entry<String, Tuple3<Long, Long, String>>> inputRight =
                    pipeline
                    .readFrom(Sources.<String, Tuple3<Long, Long, String>>map(MyConstants.IMAP_NAME_CHECKOUT));

            BatchStage<Tuple3<String, Long, Long>> inputRightRange =
                    inputRight.filter(entry -> entry.getValue().f0() >= start && entry.getValue().f0() <= end)
                    .map(entry -> Tuple3.<String, Long, Long>
                        tuple3(entry.getKey(), entry.getValue().f0(), entry.getValue().f1()));

            // Validation doesn't get outcome prior to prediction
            // Format to this style: "data,key,publish,ingest,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1"
            BatchStage<String> inputRightFormatted = inputRightRange
                    .mapUsingIMap(MyConstants.IMAP_NAME_DIGITAL_TWIN,
                            checkoutTrio -> checkoutTrio.f0(),
                            (Tuple3<String, Long, Long> checkoutTrio, Tuple3<Long, Long, String> digitalTwin) -> {
                                if (digitalTwin == null) {
                                    return null;
                                }
                                String arrPrint =
                                        Arrays.toString(MyUtils.digitalTwinCsvToBinary(null, digitalTwin.f2(), false));
                                return "data," + checkoutTrio.f0() + "," + checkoutTrio.f1() + ","
                                                + checkoutTrio.f2() + "," + arrPrint.substring(1, arrPrint.length() - 1);
                            });

            // Combine new model input and data input
            BatchStage<String> inputMerged = inputLeftFormatted.merge(inputRightFormatted);

            // Python prediction.
            BatchStage<String> pythonOutput =
                inputMerged
                .apply(PythonTransforms.mapUsingPythonBatch(
                            MyUtils.getPythonServiceConfig(PYTHON_MODULE, PYTHON_HANDLER_FN, classLoader)))
                            .setName(PYTHON_MODULE);

            // Reformat and add reality to prediction - prediction, reality
            BatchStage<Tuple2<Integer, Integer>> pythonOutputAndReality = pythonOutput
                    .map(str -> {
                        String[] tokens = str.split(",");
                        // Last field is error message, if present makes length 6
                        if (tokens.length != EXPECTED_LENGTH_OF_5) {
                            if (tokens.length != 2) {
                                // Length 2 is model training acceptance
                                log.error("Unexpected Python output: '{}'", str);
                            }
                            return null;
                        }
                        // Validation doesn't use publish timestamp, ingest timestamp or model name
                        return Tuple2.<String, Integer>tuple2(tokens[LOCATION_OF_KEY_FIELD_0],
                                Integer.valueOf(tokens[LOCATION_OF_PREDICTION_FIELD_4]));
                    })
                    .mapUsingIMap(MyConstants.IMAP_NAME_ORDERED,
                            tuple2 -> tuple2.f0(),
                            (Tuple2<String, Integer> tuple2, Object orderedValue) ->
                                Tuple2.<Integer, Integer>tuple2(tuple2.f1(), (orderedValue == null ? 0 : 1)));

            // Summarise mechanism
            AggregateOperation1<Tuple2<Integer, Integer>, ValidationAggregator, Entry<String, Double>>
                validationAggregator = ValidationAggregator.buildValidationAggregation(modelName);

            // "Take latest" aggregation
            pythonOutputAndReality
            .aggregate(validationAggregator)
            .writeTo(Sinks.map(MyConstants.IMAP_NAME_RETRAINING_ASSESSMENT));

            //MyUtils.addExponentialLogger(pythonOutputAndReality, "pythonOutputAndReality", FIVE * FIVE);
        } catch (Exception e) {
            log.error("buildPipeline()", e);
            return null;
        }

        return pipeline;
    }

    /**
     * <p>Load's a single model.
     * </p>
     *
     * @param modelName
     * @return An object, which is a String in Base64 encoding.
     */
    static BatchSource<Object> mapValue(String modelName) {
        return SourceBuilder.batch("load-" + modelName, context ->
                                new MapValueSource(context.hazelcastInstance(), MyConstants.IMAP_NAME_MODEL_VAULT, modelName))
                        .fillBufferFn(MapValueSource::fillBufferFn)
                        .build();
    }

}
