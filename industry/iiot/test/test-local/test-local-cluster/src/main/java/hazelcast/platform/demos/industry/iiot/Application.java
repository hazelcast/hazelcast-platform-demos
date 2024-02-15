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

package hazelcast.platform.demos.industry.iiot;

import java.util.concurrent.TimeUnit;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * <p>Create a Hazelcast process that runs locally, to emulate the cloud
 * if a cloud cluster is not available (eg. no Wifi).
 * </p>
 * <p>Runs for a day at most, then shuts down.
 * </p>
 */
public class Application {
    private static final String CLUSTER_NAME_PROPERTY = "CLUSTER_NAME";

    public static void main(String[] args) throws Exception {
        String clusterName = System.getProperty(CLUSTER_NAME_PROPERTY, "");
        if (clusterName.length() == 0) {
            throw new RuntimeException(String.format("No value for cluster name, property '%s'", CLUSTER_NAME_PROPERTY));
        }

        Config config = new Config();
        config.setClusterName(clusterName);
        config.getJetConfig().setEnabled(true).setResourceUploadEnabled(true);
        config.getManagementCenterConfig().setConsoleEnabled(true);

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        TimeUnit.DAYS.sleep(1L);

        hazelcastInstance.shutdown();
        System.exit(0);
    }
}
