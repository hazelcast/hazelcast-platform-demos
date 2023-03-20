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

package hazelcast.platform.demos.benchmark.nexmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.accumulator.LongLongAccumulator;
import com.hazelcast.jet.datamodel.Tuple2;

/**
 * <p>Test the latency calculator
 * </p>
 * <p>As the latency calculator uses System time, we have to be sure
 * our test timestamps are sufficiently far apart that race conditions
 * don't appear. Use {@link BenchmarkBase.WARM_U_MILLIS} and confirm
 * this is more than a trivial amount.
 * </p>
 */
public class BenchmarkBaseLatencyFnTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkBaseLatencyFnTest.class);

    private static long now;

    @BeforeAll
    public static void beforeAll() {
        now = System.currentTimeMillis();
    }

    @Test
    public void firstResultDuringWarmup(TestInfo testInfo) throws Exception {
        LongLongAccumulator state = new LongLongAccumulator();
        long lastTimestamp = 0;
        long startTimestamp = 0;
        state.set1(startTimestamp);
        state.set2(lastTimestamp);

        long currentTimestamp = startTimestamp + (BenchmarkBase.WARM_UP_MILLIS - 1);

        LOGGER.info("{} :: input :: {} & {}",
                testInfo.getDisplayName(), state, currentTimestamp);

        Tuple2<Long, Long> tuple2 = BenchmarkBase.LATENCY_FN.apply(state, currentTimestamp);

        LOGGER.info("{} :: result:: {}",
                testInfo.getDisplayName(), tuple2);

        assertThat(BenchmarkBase.WARM_UP_MILLIS).isGreaterThan(TimeUnit.SECONDS.toMillis(10));
        assertThat(tuple2).isNull();
    }

    @Test
    public void secondResultDuringWarmup(TestInfo testInfo) throws Exception {
        LongLongAccumulator state = new LongLongAccumulator();
        long lastTimestamp = BenchmarkBaseLatencyFnTest.now;
        long startTimestamp = lastTimestamp - (BenchmarkBase.WARM_UP_MILLIS - 2);
        state.set1(startTimestamp);
        state.set2(lastTimestamp);

        long currentTimestamp = lastTimestamp - 1;

        LOGGER.info("{} :: input :: {} & {}",
                testInfo.getDisplayName(), state, currentTimestamp);

        Tuple2<Long, Long> tuple2 = BenchmarkBase.LATENCY_FN.apply(state, currentTimestamp);

        LOGGER.info("{} :: result:: {}",
                testInfo.getDisplayName(), tuple2);

        assertThat(BenchmarkBase.WARM_UP_MILLIS).isGreaterThan(TimeUnit.SECONDS.toMillis(10));
        assertThat(tuple2).isNull();
    }

    @Test
    public void betterNextResultAfterWarmup(TestInfo testInfo) throws Exception {
        LongLongAccumulator state = new LongLongAccumulator();
        long lastTimestamp = BenchmarkBaseLatencyFnTest.now;
        long startTimestamp = lastTimestamp - (BenchmarkBase.WARM_UP_MILLIS + 2);
        state.set1(startTimestamp);
        state.set2(lastTimestamp);

        long currentTimestamp = lastTimestamp - 1;

        LOGGER.info("{} :: input :: {} & {}",
                testInfo.getDisplayName(), state, currentTimestamp);

        Tuple2<Long, Long> tuple2 = BenchmarkBase.LATENCY_FN.apply(state, currentTimestamp);

        LOGGER.info("{} :: result:: {}",
                testInfo.getDisplayName(), tuple2);

        assertThat(BenchmarkBase.WARM_UP_MILLIS).isGreaterThan(TimeUnit.SECONDS.toMillis(10));
        assertThat(tuple2).isNull();
    }

    @Test
    public void worseNextResultAfterWarmup(TestInfo testInfo) throws Exception {
        LongLongAccumulator state = new LongLongAccumulator();
        long lastTimestamp = BenchmarkBaseLatencyFnTest.now;
        long startTimestamp = lastTimestamp - (BenchmarkBase.WARM_UP_MILLIS + 2);
        state.set1(startTimestamp);
        state.set2(lastTimestamp);

        long currentTimestamp = lastTimestamp + 1;

        LOGGER.info("{} :: input :: {} & {}",
                testInfo.getDisplayName(), state, currentTimestamp);

        Tuple2<Long, Long> tuple2 = BenchmarkBase.LATENCY_FN.apply(state, currentTimestamp);

        LOGGER.info("{} :: result:: {}",
                testInfo.getDisplayName(), tuple2);

        assertThat(BenchmarkBase.WARM_UP_MILLIS).isGreaterThan(TimeUnit.SECONDS.toMillis(10));
        assertThat(tuple2).isNotNull();
    }

    @Test
    public void sameNextResultAfterWarmup(TestInfo testInfo) throws Exception {
        LongLongAccumulator state = new LongLongAccumulator();
        long lastTimestamp = BenchmarkBaseLatencyFnTest.now;
        long startTimestamp = lastTimestamp - (BenchmarkBase.WARM_UP_MILLIS + 2);
        state.set1(startTimestamp);
        state.set2(lastTimestamp);

        long currentTimestamp = lastTimestamp;

        LOGGER.info("{} :: input :: {} & {}",
                testInfo.getDisplayName(), state, currentTimestamp);

        Tuple2<Long, Long> tuple2 = BenchmarkBase.LATENCY_FN.apply(state, currentTimestamp);

        LOGGER.info("{} :: result:: {}",
                testInfo.getDisplayName(), tuple2);

        assertThat(BenchmarkBase.WARM_UP_MILLIS).isGreaterThan(TimeUnit.SECONDS.toMillis(10));
        assertThat(tuple2).isNull();
    }

}
