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

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;

/**
 * <p>Count input and use this for periodic retraining.
 * </p>
 */
public class RetrainingControl {
    private static final int QUADTUPLE = 4;

    /**
     * <p>Every <i>n</i> records either issue a retraining request or
     * a validation request to verify the previous model.
     * </p>
     */
    public static Pipeline buildPipeline(long buildTimestamp) {
        Pipeline pipeline = Pipeline.create();

        // Key, Timestamp and Action (clicked on checkout)
        StreamStage<Entry<String, Tuple3<Long, Long, String>>> input = pipeline
        .readFrom(Sources.<String, Tuple3<Long, Long, String>>mapJournal(
                MyConstants.IMAP_NAME_CHECKOUT,
                JournalInitialPosition.START_FROM_OLDEST)).withoutTimestamps();

        // Use grouping key so count happens on one node only
        input
        .groupingKey(__ -> "")
        .mapUsingService(
                ServiceFactories.sharedService(__ -> {
                    // Counter, first timestamp (null), last timestamp (null), previous
                    Object[] range = new Object[QUADTUPLE];
                    // So as not 0
                    range[0] = new LongAccumulator(0L);
                    return range;
                }),
                (range, key, entry) -> {
                    // Update counter, first timestamp in range, last timestamp
                    LongAccumulator longAccumulator = (LongAccumulator) range[0];
                    longAccumulator.add(1L);
                    // First / last timestamp
                    if (range[1] == null) {
                        range[1] = entry.getValue().f0();
                        range[3] = buildTimestamp;
                    }
                    range[2] = entry.getValue().f0();

                    if (longAccumulator.get() % MyConstants.RETRAINING_INTERVAL == 0) {
                        long timestamp = System.currentTimeMillis();
                        String action;
                        if (longAccumulator.get() % (2 * MyConstants.RETRAINING_INTERVAL) == 0) {
                            action = MyConstants.RETRAINING_CONTROL_ACTION_VALIDATE;
                        } else {
                            action = MyConstants.RETRAINING_CONTROL_ACTION_RETRAIN;
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("{ ");
                        stringBuilder.append(" \"action\": \"" + action + "\"");
                        stringBuilder.append(", \"timestamp\": " + timestamp);
                        stringBuilder.append(", \"count\": " + longAccumulator.get());
                        stringBuilder.append(", \"start\": " + range[1]);
                        stringBuilder.append(", \"end\": " + range[2]);
                        stringBuilder.append(", \"previous\": " + range[3]);
                        stringBuilder.append(" }");

                        HazelcastJsonValue json = new HazelcastJsonValue(stringBuilder.toString());
                        range[3] = timestamp;

                        return Tuple2.<Long, HazelcastJsonValue>tuple2(timestamp, json);
                    } else {
                        return null;
                    }
         })
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_RETRAINING_CONTROL));

        return pipeline;
    }

}
