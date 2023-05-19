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

package com.hazelcast.platform.demos.telco.churn;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * <p>Create a Hazelcast instance for Integration Tests.
 * XML config should turn off networking so don't accidentally
 * join any other running node.
 * <p>
 */
public abstract class AbstractJetIT {

    protected static HazelcastInstance hazelcastInstance;

    @BeforeAll
    public static void beforeAll() throws Exception {
        Config config = new ClasspathXmlConfig("hazelcast-test.xml");

        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
    }

    @AfterAll
    public static void afterAll() {
        hazelcastInstance.shutdown();
    }

}
