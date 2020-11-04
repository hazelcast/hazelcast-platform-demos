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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.cdc.Operation;

/**
 * <p>Test reformatting of CDC record part for Value.
 * </p>
 */
public class MySqlDebeziumOneWayCDCTest {

    private static long timestamp;
    private static Operation operation;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        timestamp = System.currentTimeMillis();
        operation = Operation.UNSPECIFIED;
    }

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    public void testNull() throws Exception {
        String input = null;

        assertThrows(NullPointerException.class,
                () -> {
                    MySqlDebeziumOneWayCDC.cdcToEntry(operation, timestamp, input);
                });
    }

    @Test
    public void testComplete() throws Exception {
        String input = "{\"id\":\"abc\",\"year\":2020,\"name\":\"def\""
                + ",\"international\":0,\"rate\":9.1,\"__op\":\"u\",\"__db\":\"churn\""
                + ",\"__table\":\"tariff\",\"__ts_ms\":1600000000000,\"__deleted\":\"false\"}";

        String expectedKey = "abc";
        HazelcastJsonValue expectedValue = new HazelcastJsonValue("{"
                + " \"id\" : \"abc\","
                + " \"year\" : 2020,"
                + " \"name\" : \"def\","
                + " \"international\" : false,"
                + " \"ratePerMinute\" : 9.1"
                + " }");

        Map.Entry<String, HazelcastJsonValue> output =
                    MySqlDebeziumOneWayCDC.cdcToEntry(operation, timestamp, input);

        assertThat(output).as("entry").isNotNull();
        assertThat(output.getKey()).as("key").isNotNull();
        assertThat(output.getKey()).as("key").isEqualTo(expectedKey);
        assertThat(output.getValue()).as("value").isNotNull();
        assertThat(output.getValue()).as("value").isEqualTo(expectedValue);
    }

    @Test
    public void testIncompleteNoKey() throws Exception {
        String input = "{\"year\":2020,\"name\":\"def\""
                + ",\"international\":0,\"rate\":9.1,\"__op\":\"u\",\"__db\":\"churn\""
                + ",\"__table\":\"tariff\",\"__ts_ms\":1600000000000,\"__deleted\":\"false\"}";

        Map.Entry<String, HazelcastJsonValue> output =
                    MySqlDebeziumOneWayCDC.cdcToEntry(operation, timestamp, input);

        assertThat(output).as("entry").isNull();
    }

    @Test
    public void testIncompleteNoYear() throws Exception {
        String input = "{\"id\":\"abc\",\"name\":\"def\""
                + ",\"international\":0,\"rate\":9.1,\"__op\":\"u\",\"__db\":\"churn\""
                + ",\"__table\":\"tariff\",\"__ts_ms\":1600000000000,\"__deleted\":\"false\"}";

        Map.Entry<String, HazelcastJsonValue> output =
                    MySqlDebeziumOneWayCDC.cdcToEntry(operation, timestamp, input);

        assertThat(output).as("entry").isNull();
    }

}
