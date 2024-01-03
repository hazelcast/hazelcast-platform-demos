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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.platform.demos.utils.UtilsFormatter;
import com.hazelcast.platform.demos.utils.UtilsViridian;

/**
 * <p>Configure client for connection to cluster. Use a config file, then override
 * if necessary.
 * </p>
 * <p>The cluster may be running in Kubernetes, Docker or direct on the host. The client
 * need not be the same (ie. cluster in Kubernetes, client in localhost) so long as it
 * can connect.
 * </p>
 */
public class ApplicationConfig {
    private static final String FILENAME = "application.properties";
    private static final String VIRIDIAN_CLUSTER1_DISCOVERY_TOKEN = "my.viridian.cluster1.discovery.token";
    private static final String VIRIDIAN_CLUSTER1_NAME = "my.viridian.cluster1.name";
    private static final String VIRIDIAN_CLUSTER1_KEY_PASSWORD = "my.viridian.cluster1.key.password";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);
    private static String clusterName = "";

    /**
     * <p>Load the configuration for this job to connect to a Jet cluster as
     * a client, from the file "{@code hazelcast-client.yml}".
     * </p>
     * <p>This file is coded on the assumption the client is running in
     * Kubernetes. If environment variables indicate others, discard the
     * declared configuration and replace it with localhost or Docker.
     * </p>
     */
    public static ClientConfig buildClientConfig() throws Exception {
        ClientConfig clientConfig = new YamlClientConfigBuilder().build();
        clusterName = clientConfig.getClusterName();

        addLabels(clientConfig);

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();
        clientNetworkConfig.getAutoDetectionConfig().setEnabled(false);

        // Use Viridian even if in Kubernetes if discovery token set
        Properties myProperties = new Properties();
        try {
            myProperties = MyUtils.loadProperties(FILENAME);
        } catch (Exception e) {
            LOGGER.error(FILENAME, e);
        }

        String useViridianStr = myProperties.getProperty(MyConstants.USE_VIRIDIAN);
        boolean useViridian = MyUtils.useViridian(useViridianStr);
        LOGGER.info("useViridian='{}'", useViridian);
        boolean kubernetesEnabled = System.getProperty("my.kubernetes.enabled", "")
                .equalsIgnoreCase("true");
        boolean dockerEnabled = System.getProperty("my.docker.enabled", "")
                .equalsIgnoreCase("true");
        boolean localhost = !dockerEnabled && !kubernetesEnabled;
        String publicAddress = System.getProperty("hazelcast.local.publicAddress", "");

        if (localhost && useViridian) {
            String message = "Localhost access not implemented for Viridian, keystore/truststore location"
                    + " not known, use Docker script instead";
            throw new RuntimeException(message);
        }

        if (useViridian) {
            UtilsViridian.configure(clientConfig,
                    myProperties.getProperty(VIRIDIAN_CLUSTER1_NAME),
                    myProperties.getProperty(VIRIDIAN_CLUSTER1_DISCOVERY_TOKEN),
                    myProperties.getProperty(VIRIDIAN_CLUSTER1_KEY_PASSWORD));

            LOGGER.info("Viridian configured, cluster id: "
                    + clientConfig.getClusterName());
        } else {
            if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
                LOGGER.info("Kubernetes configuration: service-dns: "
                        + clientNetworkConfig.getKubernetesConfig().getProperty("service-dns"));
            } else {
                clientNetworkConfig.getKubernetesConfig().setEnabled(false);

                // Can run from desktop to connect into Kubernetes
                clientNetworkConfig.setSmartRouting(false);

                if (System.getProperty("MY_HAZELCAST_SERVERS", "").length() != 0) {
                    clientNetworkConfig.setAddresses(Arrays.asList(System.getProperty("MY_HAZELCAST_SERVERS").split(",")));
                } else {
                    if (publicAddress.length() != 0) {
                        clientNetworkConfig.setAddresses(Arrays.asList(publicAddress.split(",")));
                    } else {
                        clientNetworkConfig.setAddresses(Arrays.asList("127.0.0.1"));
                    }
                }

                LOGGER.info("Non-Kubernetes configuration: member-list: "
                        + clientNetworkConfig.getAddresses());
            }
        }
        LOGGER.info("smart-routing=='{}'", clientNetworkConfig.isSmartRouting());

        return clientConfig;
    }

    public static String getClusterName() {
        return ApplicationConfig.clusterName;
    }

    /**
     * <p>For Management Center</p>
     *
     * @param clientConfig
     */
    private static void addLabels(ClientConfig clientConfig) {
        clientConfig.addLabel(System.getProperty("user.name"));
        clientConfig.addLabel(UtilsFormatter.timestampToISO8601(System.currentTimeMillis()));
    }
}
