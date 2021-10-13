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

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Junit 5
 * </p>
 */
@Slf4j
public class AccuracyAggregatorTest {
    private static final String ALGORITHM = "JUNIT";

    @Test
    public void testFinishIfNoAccumulation(TestInfo testInfo) {
        AccuracyAggregator accuracyAggregator = new AccuracyAggregator(ALGORITHM);
        Entry<String, Float> output = accuracyAggregator.exportFinish();

        log.info("{} :: output=='{}'", testInfo.getDisplayName(), output);

        assertNull(output);
    }

    @Test
    public void testBelow0To1(TestInfo testInfo) {
        Entry<String, Integer> input1
            = new SimpleImmutableEntry<String, Integer>(
                    "junit5", Integer.MIN_VALUE);

        AccuracyAggregator accuracyAggregator = new AccuracyAggregator(ALGORITHM);
        accuracyAggregator.accumulate(input1);
        Entry<String, Float> output = accuracyAggregator.exportFinish();

        log.info("{} :: input1=='{}', output=='{}'", testInfo.getDisplayName(),
                input1, output);

        Float expectedAverage = 0.0F;

        assertNotNull(output);
        assertEquals(ALGORITHM, output.getKey());
        assertEquals(expectedAverage, output.getValue());
    }

    @Test
    public void testAbove0To1(TestInfo testInfo) {
        Entry<String, Integer> input1
            = new SimpleImmutableEntry<String, Integer>(
                    "junit5", Integer.MAX_VALUE);

        AccuracyAggregator accuracyAggregator = new AccuracyAggregator(ALGORITHM);
        accuracyAggregator.accumulate(input1);
        Entry<String, Float> output = accuracyAggregator.exportFinish();

        log.info("{} :: input1=='{}', output=='{}'", testInfo.getDisplayName(),
                input1, output);

        Float expectedAverage = 0.0F;

        assertNotNull(output);
        assertEquals(ALGORITHM, output.getKey());
        assertEquals(expectedAverage, output.getValue());
    }

    @Test
    public void testAllWrong(TestInfo testInfo) {
        Entry<String, Integer> input1
            = new SimpleImmutableEntry<String, Integer>(
                "input1", 0);
        Entry<String, Integer> input2
            = new SimpleImmutableEntry<String, Integer>(
                "input2", 0);
        Entry<String, Integer> input3
            = new SimpleImmutableEntry<String, Integer>(
                "input3", 0);

        AccuracyAggregator accuracyAggregator = new AccuracyAggregator(ALGORITHM);
        accuracyAggregator.accumulate(input1);
        accuracyAggregator.accumulate(input2);
        accuracyAggregator.accumulate(input3);
        Entry<String, Float> output = accuracyAggregator.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', input3=='{}', output=='{}'", testInfo.getDisplayName(),
            input1, input2, input3, output);

        Float expectedAverage = 0.0F;

        assertNotNull(output);
        assertEquals(ALGORITHM, output.getKey());
        assertEquals(expectedAverage, output.getValue());
    }

    @Test
    public void testAllRight(TestInfo testInfo) {
        Entry<String, Integer> input1
            = new SimpleImmutableEntry<String, Integer>(
                    "input1", 1);
        Entry<String, Integer> input2
            = new SimpleImmutableEntry<String, Integer>(
                    "input2", 1);
        Entry<String, Integer> input3
            = new SimpleImmutableEntry<String, Integer>(
                    "input3", 1);

        AccuracyAggregator accuracyAggregator = new AccuracyAggregator(ALGORITHM);
        accuracyAggregator.accumulate(input1);
        accuracyAggregator.accumulate(input2);
        accuracyAggregator.accumulate(input3);
        Entry<String, Float> output = accuracyAggregator.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', input3=='{}', output=='{}'", testInfo.getDisplayName(),
                input1, input2, input3, output);

        Float expectedAverage = 1.0F;

        assertNotNull(output);
        assertEquals(ALGORITHM, output.getKey());
        assertEquals(expectedAverage, output.getValue());
    }

    @Test
    public void testSomeRight(TestInfo testInfo) {
        Entry<String, Integer> input1
            = new SimpleImmutableEntry<String, Integer>(
                "input1", 1);
        Entry<String, Integer> input2
            = new SimpleImmutableEntry<String, Integer>(
                "input2", 0);
        Entry<String, Integer> input3
            = new SimpleImmutableEntry<String, Integer>(
                "input3", 1);

        AccuracyAggregator accuracyAggregator = new AccuracyAggregator(ALGORITHM);
        accuracyAggregator.accumulate(input1);
        accuracyAggregator.accumulate(input2);
        accuracyAggregator.accumulate(input3);
        Entry<String, Float> output = accuracyAggregator.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', input3=='{}', output=='{}'", testInfo.getDisplayName(),
            input1, input2, input3, output);

        Float expectedAverage = 0.6666667F;

        assertNotNull(output);
        assertEquals(ALGORITHM, output.getKey());
        assertEquals(expectedAverage, output.getValue());
    }

    @Test
    public void testSomeRightWithCombine(TestInfo testInfo) {
        Entry<String, Integer> input1
            = new SimpleImmutableEntry<String, Integer>(
                "input1", 1);
        Entry<String, Integer> input2
            = new SimpleImmutableEntry<String, Integer>(
                "input2", 0);
        Entry<String, Integer> input3
            = new SimpleImmutableEntry<String, Integer>(
                "input3", 1);

        AccuracyAggregator accuracyAggregator1 = new AccuracyAggregator(ALGORITHM);
        AccuracyAggregator accuracyAggregator2 = new AccuracyAggregator(ALGORITHM);
        AccuracyAggregator accuracyAggregator3 = new AccuracyAggregator(ALGORITHM);
        AccuracyAggregator accuracyAggregator4 = new AccuracyAggregator(ALGORITHM);
        accuracyAggregator1.accumulate(input1);
        accuracyAggregator2.accumulate(input2);
        accuracyAggregator3.accumulate(input3);
        accuracyAggregator1.combine(accuracyAggregator2);
        accuracyAggregator1.combine(accuracyAggregator3);
        accuracyAggregator1.combine(accuracyAggregator4);
        Entry<String, Float> output = accuracyAggregator1.exportFinish();

        log.info("{} :: input1=='{}', input2=='{}', input3=='{}', output=='{}'", testInfo.getDisplayName(),
            input1, input2, input3, output);

        Float expectedAverage = 0.6666667F;

        assertNotNull(output);
        assertEquals(ALGORITHM, output.getKey());
        assertEquals(expectedAverage, output.getValue());
    }

}
