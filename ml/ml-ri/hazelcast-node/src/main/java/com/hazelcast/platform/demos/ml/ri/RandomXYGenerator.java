/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.ml.ri;

import java.util.Map;
import java.util.Map.Entry;

import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;

/**
 * <p>
 * This job creates an infinite input source, a stream of updates to a map with
 * X &amp; Y co-ordinates.
 * </p>
 */
public class RandomXYGenerator {

    private static final long LOG_THRESHOLD = 100_000L;

    /**
     * <p>
     * Mainly this job connects the input source directly to a
     * {@code com.hazelcast.map.IMap} as output.
     * </p>
     * <p>
     * To see what is happening, log every 100,000th record.
     * </p>
     */
    public static Pipeline buildPipeline() {

        Pipeline pipeline = Pipeline.create();

        StreamSource<Map.Entry<Double, Double>> mySource = SourceBuilder
                .stream(MyXYSource.class.getSimpleName(), __ -> new MyXYSource()).fillBufferFn(MyXYSource::fillBufferFn)
                .distributed(1).build();

        StreamStage<Entry<Double, Double>> inputSource = pipeline.readFrom(mySource).withoutTimestamps();

        inputSource.writeTo(Sinks.map("points"));

        /* To help with diagnostics, allow every 100,0000th item through
         * on each node. Nulls are filtered out.
         */
        inputSource
        .mapUsingService(ServiceFactories.sharedService(__ -> new LongAccumulator()),
            (counter, tuple2) -> {
                counter.subtract(1);
                if (counter.get() <= 0) {
                    counter.set(LOG_THRESHOLD);
                    return tuple2;
                }
                return null;
        }).setName("log_every_" + LOG_THRESHOLD)
        .writeTo(Sinks.logger());

        return pipeline;
    }
}
