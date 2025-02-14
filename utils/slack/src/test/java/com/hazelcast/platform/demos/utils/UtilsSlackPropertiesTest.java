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

package com.hazelcast.platform.demos.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * <p>Confirm handling of invalid properties
 * </p>
 */
public class UtilsSlackPropertiesTest {
    static final String BUILD_USER = "JUnit";
    static final String PROJECT_NAME = "Test";
    static final String NOT_NULL = "abcdefghijklmnopqrstuvwxyz";
    // Maven default in top level "pom.xml" if property not provided
    static final String UNSET = "unset";

    // Access Token invalid while Channel Name/Id valid

    @Test
    public void testSourceTokenNull(TestInfo testInfo) {
        String accessToken = null;
        String channelId = NOT_NULL;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSource(accessToken, channelId);
            });
    }

    @Test
    public void testSinkTokenNull(TestInfo testInfo) {
        String accessToken = null;
        String channelName = NOT_NULL;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSink(accessToken, channelName, PROJECT_NAME, BUILD_USER);
            });
    }

    @Test
    public void testSourceTokenZeroLength(TestInfo testInfo) {
        String accessToken = "";
        String channelId = NOT_NULL;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSource(accessToken, channelId);
            });
    }

    @Test
    public void testSinkTokenZeroLength(TestInfo testInfo) {
        String accessToken = "";
        String channelName = NOT_NULL;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSink(accessToken, channelName, PROJECT_NAME, BUILD_USER);
            });
    }

    @Test
    public void testSourceTokenShort(TestInfo testInfo) {
        String accessToken = UNSET;
        String channelId = NOT_NULL;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSource(accessToken, channelId);
            });
    }

    @Test
    public void testSinkTokenShort(TestInfo testInfo) {
        String accessToken = UNSET;
        String channelName = NOT_NULL;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSink(accessToken, channelName, PROJECT_NAME, BUILD_USER);
            });
    }

    // Channel Name/Id invalid while Access Token valid

    @Test
    public void testSourceChannelIdNull(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelId = null;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSource(accessToken, channelId);
            });
    }

    @Test
    public void testSinkChannelNameNull(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelName = null;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSink(accessToken, channelName, PROJECT_NAME, BUILD_USER);
            });
    }

    @Test
    public void testSourceChannelIdZeroLength(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelId = "";
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSource(accessToken, channelId);
            });
    }

    @Test
    public void testSinkChannelNameZeroLength(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelName = "";
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSink(accessToken, channelName, PROJECT_NAME, BUILD_USER);
            });
    }

    @Test
    public void testSourceChannelIdShort(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelId = UNSET;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSource(accessToken, channelId);
            });
    }

    @Test
    public void testSinkChannelNameShort(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelName = UNSET;
        assertThrows(RuntimeException.class, () -> {
            new UtilsSlackSink(accessToken, channelName, PROJECT_NAME, BUILD_USER);
            });
    }

    // Access Token and Channel

    @Test
    public void testSourceTokenAndChannelIdOk(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelId = NOT_NULL;
        UtilsSlackSource source = new UtilsSlackSource(accessToken, channelId);
        assertThat(source).as("source").isNotNull();
    }

    @Test
    public void testSinkTokenAndChannelNameOk(TestInfo testInfo) {
        String accessToken = NOT_NULL;
        String channelName = NOT_NULL;
        UtilsSlackSink sink = new UtilsSlackSink(accessToken, channelName, PROJECT_NAME, BUILD_USER);
        assertThat(sink).as("sink").isNotNull();
    }

}
