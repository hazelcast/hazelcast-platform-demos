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
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import com.hazelcast.function.Functions;
import com.hazelcast.function.ToLongFunctionEx;
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
 * <p>Produce statistics on the latency of predictions.
 * </p>
 * <p>TODO: Stats go to Graphite. Upgrading to Prometheus would be better.
 * More in line with Grafana 8 onwards.
 * </p>
 */
@Slf4j
public class StatisticsLatency {
    private static final long ONE_MINUTE_IN_MS = 1 * 60 * 1_000L;
    private static final float THRESHOLD_1SEC_IN_MS = 1 * 1000.0F;
    private static final long FIVE = 5L;

    public static Pipeline buildPipeline(String clusterName, String graphiteHost) {
        Pipeline pipeline = Pipeline.create();

        try {
            // Key - Input key and algorithm
            // Value - Version, Publish timestamp, Ingestion timestamp, Prediction timestamp, prediction
            StreamSourceStage<Entry<PredictionKey, Tuple5<String, Long, Long, Long, Integer>>> input =
                    pipeline.readFrom(Sources.<PredictionKey, Tuple5<String, Long, Long, Long, Integer>>mapJournal(
                            MyConstants.IMAP_NAME_PREDICTION,
                            JournalInitialPosition.START_FROM_OLDEST));

            // Use prediction timestamp
            ToLongFunctionEx<? super Entry<PredictionKey, Tuple5<String, Long, Long, Long, Integer>>>
                myTimestampFn = entry -> entry.getValue().f3();
            StreamStage<Entry<PredictionKey, Tuple5<String, Long, Long, Long, Integer>>> inputTimestamped =
                    input.withTimestamps(myTimestampFn, 0);

            // Algorithm and three timestamps
            StreamStage<Entry<String, Tuple3<Long, Long, Long>>> inputReduced
                = inputTimestamped
                    .map(entry -> new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                            entry.getKey().getAlgorithm(),
                            Tuple3.<Long, Long, Long>tuple3(
                                    entry.getValue().f1(), entry.getValue().f2(), entry.getValue().f3())
                            ));

            // Aggregate per minute, capturing latency from first two timestamps to third
            AggregateOperation1<
                Entry<String, Tuple3<Long, Long, Long>>,
                LatencyAggregator,
                Entry<String, Tuple2<Float, Float>>>
                    latencyAggregator =
                        LatencyAggregator.buildLatencyAggregation();

            StreamStage<Entry<String, Tuple2<Float, Float>>> aggregated
                = inputReduced
                .groupingKey(Functions.entryKey())
                .window(WindowDefinition.tumbling(ONE_MINUTE_IN_MS))
                .aggregate(latencyAggregator)
                .map(keyedWindowResult -> keyedWindowResult.getValue());

            if (graphiteHost.length() > 0) {
                // Two outputs, key is algorithm
                StreamStage<List<Entry<String, Float>>> formattedStats = aggregated
                .map(entry -> {
                    Tuple2<String, Float> publish =
                            Tuple2.tuple2("LATENCY." + entry.getKey().toUpperCase(Locale.ROOT) + ".PUBLISH",
                                    entry.getValue().f0());
                    Tuple2<String, Float> ingest =
                            Tuple2.tuple2("LATENCY." + entry.getKey().toUpperCase(Locale.ROOT) + ".INGEST",
                                    entry.getValue().f1());
                    // Latency huge until everything warmed up.
                    if (publish.f1() > THRESHOLD_1SEC_IN_MS || ingest.f1() > THRESHOLD_1SEC_IN_MS) {
                        log.warn("Ignore above threshold {}ms, stats pair {} {}", THRESHOLD_1SEC_IN_MS, publish, ingest);
                        return null;
                    } else {
                        List<Entry<String, Float>> stats = List.of(publish, ingest);
                        return stats;
                    }
                });

                formattedStats.writeTo(MyUtils.buildGraphiteBatchSink(graphiteHost));
                MyUtils.addExponentialLogger(formattedStats, "formattedStats", FIVE);
            } else {
                log.warn("buildPipeline(), no graphite host, using Sinks.logger()");
                aggregated.writeTo(Sinks.logger());
            }

        } catch (Exception e) {
            log.error("buildPipeline()", e);
            return null;
        }

        return pipeline;
    }

}
