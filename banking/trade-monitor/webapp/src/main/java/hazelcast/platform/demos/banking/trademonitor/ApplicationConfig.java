/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.banking.trademonitor;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.config.SSLConfig;

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
    private static final String HZ_CLOUD_CLUSTER_PASSWORD = "my.cluster1.password";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final int EXPECTED_PASSWORD_LENGTH = 11;
    private static final int EXPECTED_TOKEN_LENGTH = 50;

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

        // Use cloud even if in Kubernetes if discovery token set
        Properties myProperties = new Properties();
        try {
            myProperties = MyUtils.loadProperties(FILENAME);
        } catch (Exception e) {
            LOGGER.error(FILENAME, e);
        }

        String cloudOrHzCloud = myProperties.getProperty(MyConstants.USE_HZ_CLOUD);
        boolean useHzCloud = MyUtils.useHzCloud(cloudOrHzCloud);
        LOGGER.info("useHzCloud='{}'", useHzCloud);

        // Use cloud if property set
        if (!myProperties.getProperty(HZ_CLOUD_CLUSTER_DISCOVERY_TOKEN, "").isBlank()
                && !myProperties.getProperty(HZ_CLOUD_CLUSTER_DISCOVERY_TOKEN, "").equals("unset")
                && useHzCloud) {
            clientNetworkConfig.getKubernetesConfig().setEnabled(false);
            clientNetworkConfig.getCloudConfig().setEnabled(true);
            clientNetworkConfig.getCloudConfig()
                .setDiscoveryToken(myProperties.getProperty(HZ_CLOUD_CLUSTER_DISCOVERY_TOKEN));
            clientConfig.setClusterName(myProperties.getProperty(HZ_CLOUD_CLUSTER_NAME));

            // Cloud uses SSL
            String password = myProperties.getProperty(HZ_CLOUD_CLUSTER_PASSWORD);
            Properties sslProps = getSSLProperties(password);
            clientConfig.getNetworkConfig().setSSLConfig(new SSLConfig().setEnabled(true).setProperties(sslProps));

            if (clientConfig.getClusterName().startsWith("de-")) {
                LOGGER.info("DEV cloud");
                clientConfig.setProperty("hazelcast.client.cloud.url", "https://dev.test.hazelcast.cloud");
            }
            if (clientConfig.getClusterName().startsWith("ua-")) {
                LOGGER.info("UAT cloud");
                clientConfig.setProperty("hazelcast.client.cloud.url", "https://uat.hazelcast.cloud");
            }

            logCloudConfig(clientConfig);

            LOGGER.info("Non-Kubernetes configuration: cloud: "
                    + clientConfig.getClusterName());
        } else {
            if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
                LOGGER.info("Kubernetes configuration: service-dns: "
                        + clientNetworkConfig.getKubernetesConfig().getProperty("service-dns"));
            } else {
                clientNetworkConfig.getKubernetesConfig().setEnabled(false);

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

    /**
     * <p>Properties for SSL
     * </p>
     *
     * @param password
     * @return
     */
    private static Properties getSSLProperties(String password) {
        Properties properties = new Properties();

        properties.setProperty("javax.net.ssl.keyStore",
                new File("./client.keystore").toURI().getPath());
        properties.setProperty("javax.net.ssl.keyStorePassword", password);
        properties.setProperty("javax.net.ssl.trustStore",
                new File("./client.truststore").toURI().getPath());
        properties.setProperty("javax.net.ssl.trustStorePassword", password);

        return properties;
    }

    /**
     * <p>Confirms properties set correctly for Maven to pick up.
     * </p>
     *
     * @param clientConfig
     */
    private static void logCloudConfig(ClientConfig clientConfig) {
        LOGGER.info("Cluster name=='{}'", clientConfig.getClusterName());

        String token = Objects.toString(clientConfig.getNetworkConfig()
                .getCloudConfig().getDiscoveryToken());
        if (token.length() == EXPECTED_TOKEN_LENGTH) {
            LOGGER.info("Discovery token.length()=={}, ending '{}'", token.length(),
                    token.substring(token.length() - 1, token.length()));
        } else {
            LOGGER.warn("Discovery token.length()=={}, expected {}",
                    token.length(),
                    EXPECTED_TOKEN_LENGTH);
        }

        String password = Objects.toString(clientConfig.getNetworkConfig()
                .getSSLConfig().getProperty("javax.net.ssl.trustStorePassword"));
        if (password.length() == EXPECTED_PASSWORD_LENGTH) {
            LOGGER.info("SSL password.length()=={}, ending '{}'", password.length(),
                    password.substring(password.length() - 1, password.length()));
        } else {
            LOGGER.warn("SSL password.length()=={}, expected {}",
                    password.length(),
                    EXPECTED_PASSWORD_LENGTH);
        }
    }

}
