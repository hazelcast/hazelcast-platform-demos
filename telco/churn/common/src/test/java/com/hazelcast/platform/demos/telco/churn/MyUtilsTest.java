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

package com.hazelcast.platform.demos.telco.churn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * <p>Test {@link MUtils}, could ultimately split to one file per
 * method.
 * </p>
 */
public class MyUtilsTest {

    @Test
    public void testRot13Null() throws Exception {
        String input = null;
        String output = MyUtils.rot13(input);
        System.out.println("input=='" + input + "' output=='" + output + "'");

        assertThat(output).isNull();
    }

    @Test
    public void testRot13Blank() throws Exception {
        String input = "";
        String expected = "";
        String output = MyUtils.rot13(input);
        System.out.println("input=='" + input + "' output=='" + output + "'");

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testRot13Once() throws Exception {
        String input = "hello";
        String expected = "uryyb";
        String output = MyUtils.rot13(input);
        System.out.println("input=='" + input + "' output=='" + output + "'");

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testRot13Twice() throws Exception {
        String input = "hello";
        String expected = input;
        String output1 = MyUtils.rot13(input);
        String output2 = MyUtils.rot13(output1);
        System.out.println("input=='" + input + "' output1=='" + output1 + "' output2=='" + output2 + "'");

        assertThat(output1).isNotNull();
        assertThat(output2).isNotNull();
        assertThat(output1).isInstanceOf(String.class);
        assertThat(output2).isInstanceOf(String.class);
        assertThat(output2).isEqualTo(expected);
    }

    @Test
    public void testMakeUTF8Null() throws Exception {
        String input = null;
        String output = MyUtils.makeUTF8(input);
        System.out.println("input=='" + input + "' output=='" + output + "'");

        assertThat(output).isNull();
    }

    @Test
    public void testMakeUTF8Blank() throws Exception {
        String input = "";
        String expected = "";
        String output = MyUtils.makeUTF8(input);
        System.out.println("input=='" + input + "' output=='" + output + "'");

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testMakeUTF8DoubleQuotes() throws Exception {
        String input1 = "SELECT firstName FROM Person WHERE lastName = “Stevenson”";
        String input2 = "SELECT firstName FROM Person WHERE lastName = 'Stevenson'";
        String expected = "SELECT firstName FROM Person WHERE lastName = 'Stevenson'";

        assertThat(input1).isNotEqualTo(input2);

        String[] inputs = new String[] { input1, input2 };

        for (String input : inputs) {
            String output = MyUtils.makeUTF8(input);
            System.out.println("input=='" + input + "' output=='" + output + "'");

            assertThat(output).isNotNull();
            assertThat(output).isInstanceOf(String.class);
            assertThat(output).isEqualTo(expected);
        }
    }

    @Test
    public void testMakeUTF8SingleQuotes() throws Exception {
        String input = "SELECT firstName FROM Person WHERE lastName = ‘Stevenson’";
        String expected = "SELECT firstName FROM Person WHERE lastName = 'Stevenson'";
        String output = MyUtils.makeUTF8(input);
        System.out.println("input=='" + input + "' output=='" + output + "'");

        assertThat(output).isNotNull();
        assertThat(output).isInstanceOf(String.class);
        assertThat(output).isEqualTo(expected);
    }

}