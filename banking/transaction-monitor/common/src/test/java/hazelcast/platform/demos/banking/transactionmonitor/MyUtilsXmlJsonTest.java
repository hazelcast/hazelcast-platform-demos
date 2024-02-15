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

package hazelcast.platform.demos.banking.transactionmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * <p>Test XML payment save/restore as JSON string.
 * </p>
 */
public class MyUtilsXmlJsonTest {

    @Test
    public void testSingleXmlLineToJson(TestInfo testInfo) {
        String input = "<test yada=\"yoda\">grogu</test>";
        String expected = "<test yada=\\\"yoda\\\">grogu</test>";
        String actual = MyUtils.xmlLineToJsonString(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testMultipleXmlLineToJson(TestInfo testInfo) {
        String input = "<test yada=\"yoda\">"
                + System.lineSeparator()
                + "grogu"
                + System.lineSeparator()
                + "</test>";
        String expected =
                "[\"<test yada=\\\"yoda\\\">\""
                + ", \"grogu\""
                + ", \"</test>\""
                + "]";
        String actual = MyUtils.xmlSafeForJson(input);
        assertEquals(expected, actual);
    }

}
