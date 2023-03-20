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

package com.hazelcast.platform.demos.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>See <a href="https://github.com/junit-team/junit5/pull/2499">2499</a>,
 * until in a GA release we have to do:
 * <pre>
 * assertTrue(output instanceof String)
 * </pre>
 * <pre>
 * assertInstanceOf(object, String)
 * </pre>
 * </p>
 */
public class UtilsFormatterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsFormatterTest.class);

    @Test
    public void testMakeUTF8Null(TestInfo testInfo) throws Exception {
        String input = null;
        String output = UtilsFormatter.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertNull(output);
    }

    @Test
    public void testMakeUTF8Blank(TestInfo testInfo) throws Exception {
        String input = "";
        String expected = "";
        String output = UtilsFormatter.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertNotNull(output);
        assertTrue(output instanceof String);
        assertEquals(expected, output);
    }

    @Test
    public void testMakeUTF8DoubleQuotes(TestInfo testInfo) throws Exception {
        String input1 = "SELECT firstName FROM Person WHERE lastName = “Stevenson”";
        String input2 = "SELECT firstName FROM Person WHERE lastName = \"Stevenson\"";
        String expected = "SELECT firstName FROM Person WHERE lastName = \"Stevenson\"";

        assertNotEquals(input1, input2);

        String[] inputs = new String[] { input1, input2 };

        for (String input : inputs) {
            String output = UtilsFormatter.makeUTF8(input);
            LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

            assertNotNull(output);
            assertTrue(output instanceof String);
            assertEquals(expected, output);
        }
    }

    @Test
    public void testMakeUTF8SingleQuotes(TestInfo testInfo) throws Exception {
        String input = "SELECT firstName FROM Person WHERE lastName = ‘Stevenson’";
        String expected = "SELECT firstName FROM Person WHERE lastName = 'Stevenson'";
        String output = UtilsFormatter.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertNotNull(output);
        assertTrue(output instanceof String);
        assertEquals(expected, output);
    }

    @Test
    public void testMakeUTF8GreaterThanLessThan(TestInfo testInfo) throws Exception {
        String input = "SELECT id, callerTelno, calleeTelno, callSuccessful"
                + " FROM calls WHERE durationSeconds &gt; 0 AND durationSeconds &lt; 2";
        String expected = "SELECT id, callerTelno, calleeTelno, callSuccessful"
                + " FROM calls WHERE durationSeconds > 0 AND durationSeconds < 2";
        String output = UtilsFormatter.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

        assertNotNull(output);
        assertTrue(output instanceof String);
        assertEquals(expected, output);
    }

    @Test
    public void testMakeUTF8(TestInfo testInfo) {
        String input1 = "SELECT * FROM sentiment";
        String input2 = "SELECT __key, FLOOR(“current”) || ‘%’ AS “Churn Risk” FROM sentiment";
        String input3 = "SELECT * FROM calls";
        String input4 = "SELECT id, callerTelno, calleeTelno, callSuccessful FROM calls"
                + " WHERE durationSeconds &gt; 0";
        String expected1 = "SELECT * FROM sentiment";
        String expected2 = "SELECT __key, FLOOR(\"current\") || '%' AS \"Churn Risk\" FROM sentiment";
        String expected3 = "SELECT * FROM calls";
        String expected4 = "SELECT id, callerTelno, calleeTelno, callSuccessful FROM calls"
                + " WHERE durationSeconds > 0";

        String[] inputs = new String[] { input1, input2, input3, input4 };
        String[] expecteds = new String[] { expected1, expected2, expected3, expected4 };

        for (int i = 0; i < inputs.length; i++) {
            String output = UtilsFormatter.makeUTF8(inputs[i]);
            LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), inputs[i], output);

            assertNotNull(output);
            assertTrue(output instanceof String);
            assertEquals(expecteds[i], output);
        }
    }

    @Test
    public void testSafeForJson(TestInfo testInfo) {
      String input = "Permission (\"MapPermission\" \"mapName\" \"read\") denied!";
      String expected = "Permission ('MapPermission' 'mapName' 'read') denied!";
      String output = UtilsFormatter.safeForJsonStr(input);
      LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

      assertNotNull(output);
      assertTrue(output instanceof String);
      assertEquals(expected, output);
    }

    @Test
    public void testSafeForJsonMultiLine(TestInfo testInfo) {
      String input = "one" + System.getProperty("line.separator") + "two" + System.getProperty("line.separator") + "three";
      String expected = "one+two+three";
      String output = UtilsFormatter.safeForJsonStr(input);
      LOGGER.info("{} :: input=='{}', output=='{}'", testInfo.getDisplayName(), input, output);

      assertNotNull(output);
      assertTrue(output instanceof String);
      assertEquals(expected, output);
    }

}
