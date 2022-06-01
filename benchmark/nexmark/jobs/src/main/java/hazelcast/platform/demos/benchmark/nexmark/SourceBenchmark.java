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

package hazelcast.platform.demos.benchmark.nexmark;

import java.time.LocalTime;
import java.util.Map;

import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.WindowResult;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;

import hazelcast.platform.demos.benchmark.nexmark.model.Bid;

/**
 * <p>Validation, not part of NEXMark
 * </p>
 * <p>Confirm the source generates the required rate of events
 * </p>
 */
public class SourceBenchmark extends BenchmarkBase {
    private static final int PRICE_UNUSED = 0;

    /**
     * <p><i>Source:</i> create a stream of bids against random auction,
     * counts them and writes them out for manual validation.
     * </p>
     */
    @Override
    StreamStage<Tuple2<Long, Long>> addComputation(Pipeline pipeline, Map<String, Long> params) {
        long eventsPerSecond = params.get(BenchmarkBase.PROP_EVENTS_PER_SECOND);
        long numDistinctKeys = params.get(BenchmarkBase.PROP_NUM_DISTINCT_KEYS);
        long slideBy = params.get(BenchmarkBase.PROP_SLIDING_STEP_MILLIS);
        long windowSizeMillis = params.get(BenchmarkBase.PROP_WINDOW_SIZE_MILLIS);
        String prefix = this.getClass().getSimpleName();

        StreamStage<Bid> bids = pipeline
                .readFrom(EventSourceP.eventSource("bids", eventsPerSecond, BenchmarkBase.INITIAL_SOURCE_DELAY_MILLIS,
                        (timestamp, seq) -> new Bid(seq, timestamp, seq % numDistinctKeys, PRICE_UNUSED)))
                .withNativeTimestamps(BenchmarkBase.NO_ALLOWED_LAG);

        StreamStage<WindowResult<Long>> queryResult = bids
                .window(WindowDefinition.sliding(windowSizeMillis, slideBy))
                .aggregate(AggregateOperations.counting())
                .filter((WindowResult<Long> item) -> {
                    System.out.printf("NEXMark.%s:FILTER@%s for %d-%d => %,d items => %s%n",
                            prefix, LocalTime.now().toString(), item.start(), item.end(), item.result(), item.toString());
                    return true;
                });

        return queryResult.apply(super.determineLatency(WindowResult::end));
    }

}
