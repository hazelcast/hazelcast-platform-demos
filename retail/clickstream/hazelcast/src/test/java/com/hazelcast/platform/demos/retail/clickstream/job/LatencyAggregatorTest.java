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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Junit 5
 * </p>
 */
@Slf4j
public class LatencyAggregatorTest {

    @Test
    public void testFinishIfNoAccumulation(TestInfo testInfo) {
        LatencyAggregator latencyAggregator = new LatencyAggregator();
        Entry<String, Tuple2<Float, Float>> output = latencyAggregator.exportFinish();

        log.info("{} :: output=='{}'", testInfo.getDisplayName(),
                output);

        assertNull(output);
    }

    @Test
    public void testNegativeLatencies(TestInfo testInfo) {
        Entry<String, Tuple3<Long, Long, Long>> input1
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit5", Tuple3.<Long, Long, Long>tuple3(0L, 2L, 1L));
        Entry<String, Tuple3<Long, Long, Long>> input2
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit5", Tuple3.<Long, Long, Long>tuple3(2L, 0L, 1L));

        LatencyAggregator latencyAggregator = new LatencyAggregator();
        latencyAggregator.accumulate(input1);
        latencyAggregator.accumulate(input2);
        Entry<String, Tuple2<Float, Float>> output = latencyAggregator.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                input1, input2, output);

        String expectedKey = "junit5";
        Float expectedPublishToPredictAverage = 0.0F;
        Float expectedIngestToPredictAverage = 0.0F;

        assertNotNull(output);
        assertEquals(expectedKey, output.getKey());
        assertEquals(expectedPublishToPredictAverage, output.getValue().f0());
        assertEquals(expectedIngestToPredictAverage, output.getValue().f1());
    }

    @Test
    public void testAlgorithmChange(TestInfo testInfo) {
        Entry<String, Tuple3<Long, Long, Long>> input1
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit5", Tuple3.<Long, Long, Long>tuple3(0L, 1L, 2L));
        Entry<String, Tuple3<Long, Long, Long>> input2
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit4", Tuple3.<Long, Long, Long>tuple3(0L, 1L, 2L));

        LatencyAggregator latencyAggregator = new LatencyAggregator();
        latencyAggregator.accumulate(input1);
        latencyAggregator.accumulate(input2);
        Entry<String, Tuple2<Float, Float>> output = latencyAggregator.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                input1, input2, output);

        String expectedKey = "junit5";
        Float expectedPublishToPredictAverage = 2.0F;
        Float expectedIngestToPredictAverage = 1.0F;

        assertNotNull(output);
        assertEquals(expectedKey, output.getKey());
        assertEquals(expectedPublishToPredictAverage, output.getValue().f0());
        assertEquals(expectedIngestToPredictAverage, output.getValue().f1());
    }

    @Test
    public void testMismatchingCombine(TestInfo testInfo) {
        Entry<String, Tuple3<Long, Long, Long>> input1
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit5", Tuple3.<Long, Long, Long>tuple3(0L, 1L, 2L));
        Entry<String, Tuple3<Long, Long, Long>> input2
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit4", Tuple3.<Long, Long, Long>tuple3(0L, 1L + 1L, 2L + 2L));

        LatencyAggregator latencyAggregator1 = new LatencyAggregator();
        latencyAggregator1.accumulate(input1);
        LatencyAggregator latencyAggregator2 = new LatencyAggregator();
        latencyAggregator2.accumulate(input2);
        latencyAggregator1.combine(latencyAggregator2);
        Entry<String, Tuple2<Float, Float>> output = latencyAggregator1.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                input1, input2, output);

        String expectedKey = "junit5";
        Float expectedPublishToPredictAverage = 2.0F;
        Float expectedIngestToPredictAverage = 1.0F;

        assertNotNull(output);
        assertEquals(expectedKey, output.getKey());
        assertEquals(expectedPublishToPredictAverage, output.getValue().f0());
        assertEquals(expectedIngestToPredictAverage, output.getValue().f1());
    }

    @Test
    public void testMatchingCombine(TestInfo testInfo) {
        Entry<String, Tuple3<Long, Long, Long>> input1
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit5", Tuple3.<Long, Long, Long>tuple3(0L, 1L, 2L));
        Entry<String, Tuple3<Long, Long, Long>> input2
            = new SimpleImmutableEntry<String, Tuple3<Long, Long, Long>>(
                    "junit5", Tuple3.<Long, Long, Long>tuple3(0L, 1L + 1L, 2L + 2L));

        LatencyAggregator latencyAggregator1 = new LatencyAggregator();
        latencyAggregator1.accumulate(input1);
        LatencyAggregator latencyAggregator2 = new LatencyAggregator();
        latencyAggregator2.accumulate(input2);
        latencyAggregator1.combine(latencyAggregator2);
        Entry<String, Tuple2<Float, Float>> output = latencyAggregator1.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                input1, input2, output);

        String expectedKey = "junit5";
        Float expectedPublishToPredictAverage = 2.0F * 1.5F;
        Float expectedIngestToPredictAverage = 1.0F * 1.5F;

        assertNotNull(output);
        assertEquals(expectedKey, output.getKey());
        assertEquals(expectedPublishToPredictAverage, output.getValue().f0());
        assertEquals(expectedIngestToPredictAverage, output.getValue().f1());
    }

}
