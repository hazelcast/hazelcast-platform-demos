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

package hazelcast.platform.demos.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.version.Version;

/**
 * <p>Test checking the version is valid for the use.
 * </p>
 */
public class CheckConnectIdempotentCallableVersionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckConnectIdempotentCallableVersionTest.class);

    @Test
    public void testNull(TestInfo testInfo) throws Exception {
        Version clusterVersion = null;
        List<String> inputList = null;

        boolean actual = CheckConnectIdempotentCallable.versionCheck(clusterVersion, inputList);
        LOGGER.info("{} :: input=='{}', '{}', output=='{}'", testInfo.getDisplayName(),
                clusterVersion, inputList, actual);

        assertEquals(actual, false);
    }

    @Test
    public void testNoProperty(TestInfo testInfo) throws Exception {
        Version clusterVersion = Version.of("1.2");
        List<String> inputList = new ArrayList<>();

        boolean actual = CheckConnectIdempotentCallable.versionCheck(clusterVersion, inputList);
        LOGGER.info("{} :: input=='{}', '{}', output=='{}'", testInfo.getDisplayName(),
                clusterVersion, inputList, actual);

        assertEquals(actual, false);
    }

    @Test
    public void testBadVersionIncomplete(TestInfo testInfo) throws Exception {
        Version clusterVersion = Version.of("1.2");
        List<String> inputList = new ArrayList<>();
        inputList.add(CheckConnectIdempotentCallable.BUILD_VERSION_PROPERTY + "=1");

        boolean actual = CheckConnectIdempotentCallable.versionCheck(clusterVersion, inputList);
        LOGGER.info("{} :: input=='{}', '{}', output=='{}'", testInfo.getDisplayName(),
                clusterVersion, inputList, actual);

        assertEquals(actual, false);
    }

    @Test
    public void testBadVersionMismatch(TestInfo testInfo) throws Exception {
        Version clusterVersion = Version.of("1.2");
        List<String> inputList = new ArrayList<>();
        inputList.add(CheckConnectIdempotentCallable.BUILD_VERSION_PROPERTY + "=1.3.0");

        boolean actual = CheckConnectIdempotentCallable.versionCheck(clusterVersion, inputList);
        LOGGER.info("{} :: input=='{}', '{}', output=='{}'", testInfo.getDisplayName(),
                clusterVersion, inputList, actual);

        assertEquals(actual, false);
    }

    @Test
    public void testGoodVersion(TestInfo testInfo) throws Exception {
        Version clusterVersion = Version.of("1.3");
        List<String> inputList = new ArrayList<>();
        inputList.add(CheckConnectIdempotentCallable.BUILD_VERSION_PROPERTY + "=1.3.0");

        boolean actual = CheckConnectIdempotentCallable.versionCheck(clusterVersion, inputList);
        LOGGER.info("{} :: input=='{}', '{}', output=='{}'", testInfo.getDisplayName(),
                clusterVersion, inputList, actual);

        assertEquals(actual, true);
    }

}
