/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

import java.util.List;
import java.util.Map;

import com.hazelcast.function.ComparatorEx;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.WindowResult;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;

import hazelcast.platform.demos.benchmark.nexmark.model.Bid;

/**
 * <p>Hazelcast streaming pipeline for NEXMark query five.</p>
 * <p>From <a href="https://datalab.cs.pdx.edu/niagara/NEXMark/">here</a>
 * query five is:
 * </p>
 * <pre>
 * SELECT Rstream(auction)
 * FROM (SELECT B1.auction, count(*) AS num
 *       FROM Bid [RANGE 60 MINUTE SLIDE 1 MINUTE] B1
 *       GROUP BY B1.auction)
 * WHERE num >= ALL (SELECT count(*)
 *                   FROM Bid [RANGE 60 MINUTE SLIDE 1 MINUTE] B2
 * GROUP BY B2.auction);
 *</pre>
 */
public class Q05HotItems extends BenchmarkBase {
    private static final int PRICE_UNUSED = 0;
    private static final int TOP_10 = 10;

    /**
     * <p><i>Source:</i> create a stream of bids against random auction.
     * </p>
     * <p><i>Processing:<i> which auctions have the most bids in the time window.
     * </p>
     */
    @Override
    StreamStage<Tuple2<Long, Long>> addComputation(Pipeline pipeline, Map<String, Long> params) {
        long eventsPerSecond = params.get(BenchmarkBase.PROP_EVENTS_PER_SECOND);
        long numDistinctKeys = params.get(BenchmarkBase.PROP_NUM_DISTINCT_KEYS);
        long slideBy = params.get(BenchmarkBase.PROP_SLIDING_STEP_MILLIS);
        long windowSizeMillis = params.get(BenchmarkBase.PROP_WINDOW_SIZE_MILLIS);

        StreamStage<Bid> bids = pipeline
                .readFrom(EventSourceP.eventSource("bids", eventsPerSecond, BenchmarkBase.INITIAL_SOURCE_DELAY_MILLIS,
                        (timestamp, seq) -> new Bid(seq, timestamp, seq % numDistinctKeys, PRICE_UNUSED)))
                .withNativeTimestamps(BenchmarkBase.NO_ALLOWED_LAG);

        // NEXMark Query 5 start
        StreamStage<WindowResult<List<KeyedWindowResult<Long, Long>>>> queryResult = bids
                .window(WindowDefinition.sliding(windowSizeMillis, slideBy))
                .groupingKey(Bid::auctionId)
                .aggregate(AggregateOperations.counting())
                .window(WindowDefinition.tumbling(slideBy))
                .aggregate(AggregateOperations.topN(TOP_10, ComparatorEx.comparing(KeyedWindowResult::result)));
        // NEXMark Query 5 end

        return queryResult.apply(super.determineLatency(WindowResult::end));
    }

}
