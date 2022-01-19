/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.retail.clickstream.ClickstreamKey;
import com.hazelcast.platform.demos.retail.clickstream.CsvField;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;

/**
 * <p>Takes the raw clickstream and routes event by type to one
 * of three maps.
 * </p>
 * <p>Events are pre-checkout events, such as "add to basket",
 * going to checkout, and buying. See {@link CsvField}
 * </p>
 */
public class ClickstreamHandler {

    public static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();

        // Key is combo of UserID & Timestamp. Value is Action text
        StreamStage<Entry<ClickstreamKey, String>> input = pipeline
        .readFrom(Sources.<ClickstreamKey, String>mapJournal(
                MyConstants.IMAP_NAME_CLICKSTREAM,
                JournalInitialPosition.START_FROM_OLDEST)).withoutTimestamps();

        // UserId, Pulsar Timestamp, Hazelcast Timestamp, Action Text
        StreamStage<Tuple4<String, Long, Long, CsvField>> reformatted = input
        .map(entry -> {
            return Tuple4.<String, Long, Long, CsvField>tuple4(entry.getKey().getKey(),
                    entry.getKey().getPublishTimestamp(),
                    entry.getKey().getIngestTimestamp(),
                    CsvField.valueOf(entry.getValue()));
        });

        // Value is now a tuple of 2 * Timestamp and Action Text
        reformatted
        .filter(tuple4 -> tuple4.f3() == CsvField.checkout)
        .map(tuple4 -> Tuple2.tuple2(tuple4.f0(),
                Tuple3.tuple3(tuple4.f1(), tuple4.f2(), tuple4.f3().toString())))
        .setName(CsvField.checkout.toString())
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CHECKOUT));

        // Value is now a tuple of 2 * Timestamp and Action Text
        reformatted
        .filter(tuple4 -> tuple4.f3() == CsvField.ordered)
        .map(tuple4 -> Tuple2.tuple2(tuple4.f0(),
                Tuple3.tuple3(tuple4.f1(), tuple4.f2(), tuple4.f3().toString())))
        .setName(CsvField.ordered.toString())
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_ORDERED));

        // Value is now a tuple of 2 * Timestamp and *concatenated* Action Text. Note: includes "ordered"
        reformatted
        .filter(tuple4 -> tuple4.f3() != CsvField.checkout)
        .map(tuple4 -> Tuple2.tuple2(tuple4.f0(),
                Tuple3.tuple3(tuple4.f1(), tuple4.f2(), tuple4.f3().toString())))
        .writeTo(Sinks.mapWithMerging(MyConstants.IMAP_NAME_DIGITAL_TWIN,
                (oldValue, newValue) -> {
                    // No field is substring of another. Ignore duplicates, we don't count how often each button is clicked.
                    if (oldValue.f2().contains(newValue.f2())) {
                        return Tuple3.tuple3(newValue.f0(), newValue.f1(), oldValue.f2());
                    }
                    // Timestamp from newest
                    return Tuple3.tuple3(newValue.f0(), newValue.f1(),
                            oldValue.f2() + "," + newValue.f2());
                }));

        return pipeline;
    }
}
