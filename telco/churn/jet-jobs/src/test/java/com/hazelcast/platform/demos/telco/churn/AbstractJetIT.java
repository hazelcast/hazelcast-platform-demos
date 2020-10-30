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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;

/**
 * <p>Create an standlone Jet instance for Integration Tests.
 * XML config should turn off networking so don't accidentally
 * join any other running node.
 * <p>
 */
public abstract class AbstractJetIT {

    protected static JetInstance jetInstance;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws Exception {
        Config config = new ClasspathXmlConfig("hazelcast-test.xml");

        JetConfig jetConfig = new JetConfig();
        jetConfig.setHazelcastConfig(config);

        jetInstance = Jet.newJetInstance(jetConfig);
    }

    @AfterClass
    public static void afterClass() {
        jetInstance.shutdown();
    }

}
