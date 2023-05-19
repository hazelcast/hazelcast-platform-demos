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

package com.hazelcast.platform.demos.ml.ri;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.python.PythonServiceConfig;

/**
 * <p>Create an un-clustered Jet instance for Integration Tests
 * <p>
 */
public abstract class AbstractJetIT {

    protected static HazelcastInstance hazelcastInstance;
    protected static PythonServiceConfig pythonServiceConfig;

    @BeforeAll
    public static void beforeAll() throws Exception {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);

        config.getJetConfig().setResourceUploadEnabled(true).setEnabled(true);

        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
    }

    @AfterAll
    public static void afterAll() {
        hazelcastInstance.shutdown();
    }

}
