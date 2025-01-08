/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Junit 5
 * </p>
 */
@Slf4j
public class MyUtilsParseTest {

    @Test
    public void testParseNull(TestInfo testInfo) throws Exception {
        String input = null;
        long output = MyUtils.parseTimestamp(input);
        long expected = 0;
        log.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertEquals(expected, output);
    }

    @Test
    public void testParseEmpty(TestInfo testInfo) throws Exception {
        String input = "";
        long output = MyUtils.parseTimestamp(input);
        long expected = 0;
        log.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertEquals(expected, output);
    }

    @Test
    public void testParseInvalid(TestInfo testInfo) throws Exception {
        String input = "2021-02-29T00:00:00Z";
        long output = MyUtils.parseTimestamp(input);
        long expected = 0;
        log.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertEquals(expected, output);
    }

    @Test
    public void testParseValid(TestInfo testInfo) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        long expected = now.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        String input = now.format(DateTimeFormatter.ISO_DATE_TIME);
        long output = MyUtils.parseTimestamp(input);
        log.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertEquals(expected, output);
    }

}
