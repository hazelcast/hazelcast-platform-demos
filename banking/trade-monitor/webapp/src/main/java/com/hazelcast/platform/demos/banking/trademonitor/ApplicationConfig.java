/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String FILENAME = "application.properties";
    private static final String HZ_CLOUD_CLUSTER_DISCOVERY_TOKEN = "my.cluster1.discovery-token";
    private static final String HZ_CLOUD_CLUSTER_NAME = "my.cluster1.name";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    /**
     * <p>Load the configuration for this job to connect to a Jet cluster as
     * a client, from the file "{@code hazelcast-client.yml}".
     * </p>
     * <p>This file is coded on the assumption the client is running in
     * Kubernetes. If environment variables indicate others, discard the
     * declared configuration and replace it with localhost or Docker.
     * </p>
     */
    public static ClientConfig buildJetClientConfig() {
        ClientConfig clientConfig = new YamlClientConfigBuilder().build();

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();
        clientNetworkConfig.getAutoDetectionConfig().setEnabled(false);

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("Kubernetes configuration: service-dns: "
                    + clientNetworkConfig.getKubernetesConfig().getProperty("service-dns"));
        } else {
            clientNetworkConfig.getKubernetesConfig().setEnabled(false);

            Properties myProperties = new Properties();
            try {
                myProperties = MyUtils.loadProperties(FILENAME);
            } catch (Exception e) {
                LOGGER.error(FILENAME, e);
            }

            // Use cloud if property set
            if (!myProperties.getProperty(HZ_CLOUD_CLUSTER_DISCOVERY_TOKEN, "").isBlank()) {
                clientNetworkConfig.getCloudConfig().setEnabled(true);
                clientNetworkConfig.getCloudConfig()
                    .setDiscoveryToken(myProperties.getProperty(HZ_CLOUD_CLUSTER_DISCOVERY_TOKEN));
                clientConfig.setClusterName(myProperties.getProperty(HZ_CLOUD_CLUSTER_NAME));

                if (clientConfig.getClusterName().startsWith("de-")) {
                    LOGGER.info("DEV cloud");
                    clientConfig.setProperty("hazelcast.client.cloud.url", "https://dev.test.hazelcast.cloud");
                }
                if (clientConfig.getClusterName().startsWith("ua-")) {
                    LOGGER.info("UAT cloud");
                    clientConfig.setProperty("hazelcast.client.cloud.url", "https://uat.hazelcast.cloud");
                }

                LOGGER.info("Non-Kubernetes configuration: cloud: "
                        + clientConfig.getClusterName());

            } else {
                if (System.getProperty("MY_HAZELCAST_SERVERS", "").length() != 0) {
                    clientNetworkConfig.setAddresses(Arrays.asList(System.getProperty("MY_HAZELCAST_SERVERS").split(",")));
                } else {
                    if (System.getProperty("hazelcast.local.publicAddress", "").length() != 0) {
                        clientNetworkConfig.setAddresses(Arrays.asList(
                                System.getProperty("hazelcast.local.publicAddress").split(",")));
                    } else {
                        clientNetworkConfig.setAddresses(Arrays.asList("127.0.0.1"));
                    }
                }

                LOGGER.info("Non-Kubernetes configuration: member-list: "
                        + clientNetworkConfig.getAddresses());
            }
        }


        return clientConfig;
    }

}
