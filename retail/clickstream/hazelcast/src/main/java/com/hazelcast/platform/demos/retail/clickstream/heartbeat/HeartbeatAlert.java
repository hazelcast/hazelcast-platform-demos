/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream.heartbeat;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Objects;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StageWithKeyAndWindow;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;

/**
 * <p>Log an alert if heartbeats from other cluster(s) are significantly
 * behind. Assumes clocks are reasonably correct.
 * </p>
 */
public class HeartbeatAlert {
    // Heartbeats are every minute, should see at least one in minute and a half
    private static final long ONE_MINUTE_IN_MS = 60 * 1_000L;
    private static final long NINETY_SECONDS_IN_MS = 90 * 1_000L;

    public static Pipeline buildPipeline(String localClusterName) {

        Pipeline pipeline = Pipeline.create();

        // (1) Live stream of heartbeats
        StreamStage<Entry<String, HazelcastJsonValue>> input = pipeline
        .readFrom(Sources.<String, HazelcastJsonValue>mapJournal(
                MyConstants.IMAP_NAME_HEARTBEAT,
                JournalInitialPosition.START_FROM_OLDEST)).withIngestionTimestamps();

        // (2) Group heartbeats in windows
        StageWithKeyAndWindow<Entry<String, HazelcastJsonValue>, String> window = input
        .groupingKey(entry -> entry.getKey().split("\\.")[0])
        .window(WindowDefinition.tumbling(NINETY_SECONDS_IN_MS));

        AggregateOperation1<
            Entry<String, HazelcastJsonValue>,
            HeartbeatAlertAggregator,
            Entry<Long, String>>
            heartbeatAlertAggregator =
                HeartbeatAlertAggregator.buildHeartbeatAlertAggregation(localClusterName);

        // (3) Collate all heartbeats in a window to find the latest
        StreamStage<Entry<Long, String>> alerts = window
        .aggregate(heartbeatAlertAggregator)
        .map(keyedWindowResult -> {
            long end = keyedWindowResult.end();
            long timestamp = keyedWindowResult.getValue().getKey();
            String member = Objects.toString(keyedWindowResult.getValue().getValue());
            long diff = end - timestamp;
            if (timestamp == 0) {
                return new SimpleImmutableEntry<>(end, "No heartbeat");
            }
            if (diff < ONE_MINUTE_IN_MS) {
                // Null means filter out
                return null;
            }
            String message = String.format("Last timestamp from '%s', late by %dms",
                    member, diff);
            return new SimpleImmutableEntry<>(end, message);
        });

        // (4) Save result based on the last time received. This map has a map listener
        alerts
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_ALERT));

        return pipeline;
    }

}
