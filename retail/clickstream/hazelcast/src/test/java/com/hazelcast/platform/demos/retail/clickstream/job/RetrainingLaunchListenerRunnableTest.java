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

package com.hazelcast.platform.demos.retail.clickstream.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Junit 5
 * </p>
 */
@Slf4j
public class RetrainingLaunchListenerRunnableTest {

    @Test
    public void testFindNum10000True(TestInfo testInfo) throws Exception {
        long count = 10000;
        boolean isValidate = true;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "0";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }
    @Test
    public void testFindNum10000False(TestInfo testInfo) throws Exception {
        long count = 10000;
        boolean isValidate = false;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "1";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }

    @Test
    public void testFindNum15000True(TestInfo testInfo) throws Exception {
        long count = 15000;
        boolean isValidate = true;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "0";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }
    @Test
    public void testFindNum15000False(TestInfo testInfo) throws Exception {
        long count = 15000;
        boolean isValidate = false;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "1";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }

    @Test
    public void testFindNum20000True(TestInfo testInfo) throws Exception {
        long count = 20000;
        boolean isValidate = true;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "1";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }
    @Test
    public void testFindNum20000False(TestInfo testInfo) throws Exception {
        long count = 20000;
        boolean isValidate = false;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "1";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }

    @Test
    public void testFindNum30000True(TestInfo testInfo) throws Exception {
        long count = 30000;
        boolean isValidate = true;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "1";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }
    @Test
    public void testFindNum30000False(TestInfo testInfo) throws Exception {
        long count = 30000;
        boolean isValidate = false;
        String output = RetrainingLaunchListenerRunnable.findNum(count, isValidate);
        String expected = "2";
        log.info("{} :: input1=='{}', input2=='{}', output=='{}'", testInfo.getDisplayName(),
                count, isValidate, output);

        assertNotNull(output);
        assertEquals(expected, output);
    }

}
