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

import java.util.Arrays;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;

/**
 * <p>Configure Jet client for connection to cluster. Use a config file, then override
 * if necessary.
 * </p>
 * <p>The cluster may be running in Kubernetes, Docker or direct on the host. The client
 * need not be the same (ie. cluster in Kubernetes, client in localhost) so long as it
 * can connect.
 * </p>
 */
public class ApplicationConfig {

    /**
     * <p>Load the configuration for this job to connect to a Jet cluster as
     * a client, from the file "{@code hazelcast-client.yml}".
     * </p>
     * <p>This file is coded on the assumption the client is running in
     * Kubernetes. If environment variables indicate others, discard the
     * declared configuration and replace it with localhost or Docker.
     * </p>
     */
    public static ClientConfig buildClientConfig() {
        ClientConfig clientConfig = new YamlClientConfigBuilder().build();

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            System.out.println("Kubernetes configuration: service-dns: "
                    + clientNetworkConfig.getKubernetesConfig().getProperty("service-dns"));
        } else {
            clientNetworkConfig.getKubernetesConfig().setEnabled(false);

            if (System.getProperty("hazelcast.local.publicAddress", "").length() != 0) {
                clientNetworkConfig.setAddresses(Arrays.asList(System.getProperty("hazelcast.local.publicAddress")));
            } else {
                clientNetworkConfig.setAddresses(Arrays.asList("127.0.0.1"));
            }

            System.out.println("Non-Kubernetes configuration: member-list: "
                    + clientNetworkConfig.getAddresses());
        }


        return clientConfig;
    }

}
