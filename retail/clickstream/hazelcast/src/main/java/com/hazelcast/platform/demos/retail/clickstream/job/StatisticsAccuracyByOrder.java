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

import java.util.Map.Entry;

import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple5;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSourceStage;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.PredictionKey;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Produce statistics on the accuracy of predictions.
 * </p>
 * <p><b>NOTE</b> This evaluation uses "order" as input, not "checkout".
 * If the customer has ordered, we compare against the prediction. This
 * excludes cases where the customer didn't order, as there is no order
 * record to drive the accuracy calculation.
 * </p>
 * <p>But see also {@link RandomForestValidation} which does a different
 * variation of the calculation.
 * </p>
 */
@Slf4j
public class StatisticsAccuracyByOrder {
    private static final String[] ALGORITHMS = { "DecisionTree", "Gaussian", "RandomForest" };
    //XXX private static final long ONE_MINUTE_IN_MS = 1 * 60 * 1_000L;

    public static Pipeline buildPipeline(String clusterName, String graphiteHost) {
        Pipeline pipeline = Pipeline.create();

        try {
            // Key - Input key
            // Value - Publish timestamp, Ingestion timestamp, action ("order")
            StreamSourceStage<Entry<String, Tuple3<Long, Long, String>>> input =
                    pipeline.readFrom(Sources.<String, Tuple3<Long, Long, String>>mapJournal(
                            MyConstants.IMAP_NAME_ORDERED,
                            JournalInitialPosition.START_FROM_OLDEST));

            // Use output timestamp in event journal for windowing, not input timestamps in tuple
            StreamStage<Entry<String, Tuple3<Long, Long, String>>> inputTimestamped =
                    input
                    .withIngestionTimestamps();

            // 3 way repeat for the possible algorithms
            for (String algorithm : ALGORITHMS) {
                inputTimestamped
                .mapUsingIMap(MyConstants.IMAP_NAME_PREDICTION,
                        entry -> new PredictionKey(entry.getKey(), algorithm),
                        (Entry<String, Tuple3<Long, Long, String>> entry,
                                Tuple5<String, Long, Long, Long, Integer> predictionValue) -> {
                                    if (predictionValue == null) {
                                        return null;
                                    }
                                    String s =
                                    "ALGO-2 K" + entry.getKey() + "A" + algorithm + "V" + predictionValue;
                                    log.info(s);
                                    //FIXME Grafana password and default dashboard -- try DOCKER
                                    //FIXME Grafana password and default dashboard -- try DOCKER
                                    //FIXME Grafana password and default dashboard -- try DOCKER
                                    //FIXME Grafana password and default dashboard -- try DOCKER
                                    //FIXME Grafana password and default dashboard -- try DOCKER
                                    //FIXME ALGO-2 K65a5-774d5497-65a5-4603-a825-833262
                                    //FIXME ADecisionTree
                                    //FIXME V(2021-10-01T15:27:59Z, 1633103327410, 1633103327429, 1633103327439, 0)
                                    return s;
                                }
                                ).setName("prediction-" + algorithm)
                .filter(x -> false)
                .writeTo(Sinks.logger());
            }
        } catch (Exception e) {
            log.error("buildPipeline()", e);
            return null;
        }

        return pipeline;
    }


}
