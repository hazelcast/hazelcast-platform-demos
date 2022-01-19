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

package com.hazelcast.platform.demos.telco.churn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Test {@link MUtils}, could ultimately split to one file per
 * method.
 * </p>
 */
public class MyUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyUtilsTest.class);

    @Test
    public void testRot13Null(TestInfo testInfo) throws Exception {
        String input = null;
        String output = MyUtils.rot13(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNull();
    }

    @Test
    public void testRot13Blank(TestInfo testInfo) throws Exception {
        String input = "";
        String expected = "";
        String output = MyUtils.rot13(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testRot13Once(TestInfo testInfo) throws Exception {
        String input = "hello";
        String expected = "uryyb";
        String output = MyUtils.rot13(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testRot13Twice(TestInfo testInfo) throws Exception {
        String input = "hello";
        String expected = input;
        String output1 = MyUtils.rot13(input);
        String output2 = MyUtils.rot13(output1);
        LOGGER.info("{} :: input=='{}', output1=='{}', output2=='{}'",
                testInfo.getDisplayName(), input, output1, output2);

        assertThat(output1).isNotNull();
        assertThat(output2).isNotNull();
        assertThat(output1).isInstanceOf(String.class);
        assertThat(output2).isInstanceOf(String.class);
        assertThat(output2).isEqualTo(expected);
    }

    @Test
    public void testMakeUTF8Null(TestInfo testInfo) throws Exception {
        String input = null;
        String output = MyUtils.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNull();
    }

    @Test
    public void testMakeUTF8Blank(TestInfo testInfo) throws Exception {
        String input = "";
        String expected = "";
        String output = MyUtils.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testMakeUTF8DoubleQuotes(TestInfo testInfo) throws Exception {
        String input1 = "SELECT firstName FROM Person WHERE lastName = “Stevenson”";
        String input2 = "SELECT firstName FROM Person WHERE lastName = \"Stevenson\"";
        String expected = "SELECT firstName FROM Person WHERE lastName = \"Stevenson\"";

        assertThat(input1).isNotEqualTo(input2);

        String[] inputs = new String[] { input1, input2 };

        for (String input : inputs) {
            String output = MyUtils.makeUTF8(input);
            LOGGER.info("{} :: input=='{}', output=='{}'",
                    testInfo.getDisplayName(), input, output);

            assertThat(output).isNotNull();
            assertThat(output).isInstanceOf(String.class);
            assertThat(output).isEqualTo(expected);
        }
    }

    @Test
    public void testMakeUTF8SingleQuotes(TestInfo testInfo) throws Exception {
        String input = "SELECT firstName FROM Person WHERE lastName = ‘Stevenson’";
        String expected = "SELECT firstName FROM Person WHERE lastName = 'Stevenson'";
        String output = MyUtils.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testMakeUTF8GreaterThanLessThan(TestInfo testInfo) throws Exception {
        String input = "SELECT id, callerTelno, calleeTelno, callSuccessful"
                + " FROM calls WHERE durationSeconds &gt; 0 AND durationSeconds &lt; 2";
        String expected = "SELECT id, callerTelno, calleeTelno, callSuccessful"
                + " FROM calls WHERE durationSeconds > 0 AND durationSeconds < 2";
        String output = MyUtils.makeUTF8(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
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
            String output = MyUtils.makeUTF8(inputs[i]);
            LOGGER.info("{} :: input=='{}', output=='{}'",
                    testInfo.getDisplayName(), inputs[i], output);

            assertThat(output).isNotNull();
            assertThat(output).isInstanceOf(String.class);
            assertThat(output).isEqualTo(expecteds[i]);
        }
    }

    @Test
    public void testSafeForJson(TestInfo testInfo) {
        String input = "Permission (\"MapPermission\" \"mapName\" \"read\") denied!";
        String expected = "Permission ('MapPermission' 'mapName' 'read') denied!";
        String output = MyUtils.safeForJsonStr(input);
        LOGGER.info("{} :: input=='{}', output=='{}'",
                testInfo.getDisplayName(), input, output);

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

}
