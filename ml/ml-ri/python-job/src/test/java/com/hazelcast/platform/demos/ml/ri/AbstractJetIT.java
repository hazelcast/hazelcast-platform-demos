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

package com.hazelcast.platform.demos.ml.ri;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.hazelcast.config.Config;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.python.PythonServiceConfig;

/**
 * <p>Create an un-clustered Jet instance for Integration Tests
 * <p>
 */
public abstract class AbstractJetIT {

    protected static JetInstance jetInstance;
    protected static PythonServiceConfig pythonServiceConfig;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws Exception {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);

        JetConfig jetConfig = new JetConfig();
        jetConfig.setHazelcastConfig(config);

        jetInstance = Jet.newJetInstance(jetConfig);
    }

    @AfterClass
    public static void afterClass() {
        jetInstance.shutdown();
    }

}
