/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.python.PythonTransforms;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Pass a block of input to the training algorithm to produce
 * a model.
 * </p>
 * TODO: Difficult to stop RandomForest model exceeding 4MB, but
 * see https://github.com/hazelcast/hazelcast/issues/19503 and
 * model size threshold in Python code.
 */
@Slf4j
public class RandomForestRetraining {
    private static final String PYTHON_MODULE = "randomforest_train";
    private static final String PYTHON_HANDLER_FN = "train_model";
    //private static final long FIVE = 5L;

    public static Pipeline buildPipeline(long start, long end, String modelName) {
        Pipeline pipeline = Pipeline.create();

        try {
            BatchStage<Entry<String, Tuple3<Long, Long, String>>> input =
                    pipeline
                    .readFrom(Sources.<String, Tuple3<Long, Long, String>>map(
                            MyConstants.IMAP_NAME_CHECKOUT));

            BatchStage<String> inputRange =
                    input
                    .filter(entry -> entry.getValue().f0() >= start && entry.getValue().f0() <= end)
                    .map(Entry::getKey);

            // Convert words to 0 or 1 for actions. Training doesn't need the key but needs to know buy or not
            BatchStage<String> inputRangeReformatted =
                    inputRange
                    .mapUsingIMap(MyConstants.IMAP_NAME_DIGITAL_TWIN,
                            FunctionEx.identity(),
                            (String key, Tuple3<Long, Long, String> digitalTwin) ->
                            MyUtils.digitalTwinCsvToBinary(null, digitalTwin.f2(), true))
                    .map(arr -> Arrays.toString(arr));

            // Group all onto any one node, only run training once per cluster
            BatchStage<String> inputRangeReformattedSingleton =
                    inputRangeReformatted
                    .groupingKey(__ -> "")
                    .mapUsingService(ServiceFactories.sharedService(__ -> null),
                            (unusedService, unusedKey, value) -> value);

            // Input looks like "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0]"
            BatchStage<String> pythonOutput =
                    inputRangeReformattedSingleton
                    .apply(PythonTransforms.mapUsingPythonBatch(
                            MyUtils.getPythonServiceConfig(PYTHON_MODULE, PYTHON_HANDLER_FN)))
                                .setLocalParallelism(1)
                                .setName(PYTHON_MODULE)
                    .filter(line -> line.length() > 0);

            AggregateOperation1<String, ModelAggregator, Entry<String, String>>
                modelAggregator = ModelAggregator.buildModelAggregation(modelName);

            // "Take latest" aggregation
            pythonOutput
            .aggregate(modelAggregator)
            .writeTo(Sinks.map(MyConstants.IMAP_NAME_MODEL_VAULT));

            //MyUtils.addExponentialLogger(pythonOutput, "pythonOutput", FIVE);
        } catch (Exception e) {
            log.error("buildPipeline()", e);
            return null;
        }

        return pipeline;
    }

}
