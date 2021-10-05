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

import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import com.hazelcast.function.Functions;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple5;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSourceStage;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;
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
    private static final long ONE_MINUTE_IN_MS = 1 * 60 * 1_000L;
    private static final long FIVE = 5L;

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
                StreamStage<Tuple2<String, Integer>> inputReduced =
                    inputTimestamped
                    .mapUsingIMap(MyConstants.IMAP_NAME_PREDICTION,
                        entry -> new PredictionKey(entry.getKey(), algorithm),
                        (Entry<String, Tuple3<Long, Long, String>> entry,
                                Tuple5<String, Long, Long, Long, Integer> predictionValue) -> {
                                    if (predictionValue == null) {
                                        return null;
                                    }
                                    return Tuple2.<String, Integer>tuple2(entry.getKey(), predictionValue.f4());
                                }
                                ).setName("prediction-" + algorithm);

                StatisticsAccuracyByOrder.addAggregation(inputReduced, algorithm, graphiteHost);
            }
        } catch (Exception e) {
            log.error("buildPipeline()", e);
            return null;
        }

        return pipeline;
    }

    /**
     * <p>Split off standard coding to stop method size being too huge.
     * Aggregate by accuracy, format and send to Graphite for Grafana.
     * </p>
     *
     * @param inputReduced
     * @param algorithm
     * @param graphiteHost
     */
    private static void addAggregation(StreamStage<Tuple2<String, Integer>> inputReduced,
            String algorithm, String graphiteHost) {

        AggregateOperation1<
            Entry<String, Integer>,
            AccuracyAggregator,
            Entry<String, Float>>
                accuracyAggregator =
                    AccuracyAggregator.builAccuracyAggregation(algorithm);

        StreamStage<Entry<String, Float>> aggregated
            = inputReduced
            .groupingKey(Functions.entryKey())
            .window(WindowDefinition.tumbling(ONE_MINUTE_IN_MS))
            .aggregate(accuracyAggregator).setName("aggregate-" + algorithm)
            .map(keyedWindowResult -> keyedWindowResult.getValue()).setName("getAccuracy-" + algorithm);

        if (graphiteHost.length() > 0) {
            // Key is algorithm
            StreamStage<List<Entry<String, Float>>> formattedStats = aggregated
            .map(entry -> {
                Tuple2<String, Float> accuracy =
                        Tuple2.tuple2("ACCURACY." + entry.getKey().toUpperCase(Locale.ROOT),
                                entry.getValue());
                List<Entry<String, Float>> stats = List.of(accuracy);
                return stats;
             }).setName("accuracy-" + algorithm);

            formattedStats.writeTo(MyUtils.buildGraphiteBatchSink(graphiteHost));
            MyUtils.addExponentialLogger(formattedStats, "formattedStats-" + algorithm, FIVE);
        } else {
            log.warn("buildPipeline(), no graphite host, using Sinks.logger()");
            aggregated.writeTo(Sinks.logger()).setName("logger-" + algorithm);
        }
    }


}
