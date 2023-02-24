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

package hazelcast.platform.demos.banking.transactionmonitor;

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
import com.hazelcast.platform.demos.utils.UtilsFormatter;

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
    private static final String VIRIDIAN_CLUSTER1_DISCOVERY_TOKEN = "my.viridian.cluster1.discovery.token";
    private static final String VIRIDIAN_CLUSTER1_ID = "my.viridian.cluster1.id";
    private static final String VIRIDIAN_CLUSTER1_KEYSTORE_PASSWORD = "my.viridian.cluster1.keystore.password";
    private static final String VIRIDIAN_CLUSTER1_TRUSTSTORE_PASSWORD = "my.viridian.cluster1.truststore.password";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final int EXPECTED_PASSWORD_LENGTH = 11;
    private static final int EXPECTED_TOKEN_LENGTH = 50;
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
    public static ClientConfig buildJetClientConfig() {
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

        String kubernetesOrViridian = myProperties.getProperty(MyConstants.USE_VIRIDIAN);
        boolean useViridian = MyUtils.useViridian(kubernetesOrViridian);
        LOGGER.info("useViridian='{}'", useViridian);

        // Use Viridian if property set
        if (!myProperties.getProperty(VIRIDIAN_CLUSTER1_DISCOVERY_TOKEN, "").isBlank()
                && !myProperties.getProperty(VIRIDIAN_CLUSTER1_DISCOVERY_TOKEN, "").equals("unset")
                && useViridian) {
            clientNetworkConfig.setSmartRouting(false);
            clientNetworkConfig.getKubernetesConfig().setEnabled(false);
            clientNetworkConfig.getCloudConfig().setEnabled(true);
            clientNetworkConfig.getCloudConfig()
                .setDiscoveryToken(myProperties.getProperty(VIRIDIAN_CLUSTER1_DISCOVERY_TOKEN));
            clientConfig.setClusterName(myProperties.getProperty(VIRIDIAN_CLUSTER1_ID));

            // Viridian uses SSL
            String keystorePassword = myProperties.getProperty(VIRIDIAN_CLUSTER1_KEYSTORE_PASSWORD);
            String truststorePassword = myProperties.getProperty(VIRIDIAN_CLUSTER1_TRUSTSTORE_PASSWORD);
            Properties sslProps = getSSLProperties(keystorePassword, truststorePassword);
            clientConfig.getNetworkConfig().setSSLConfig(new SSLConfig().setEnabled(true).setProperties(sslProps));

            // Viridian
            clientConfig.setProperty("hazelcast.client.cloud.url", "https://api.viridian.hazelcast.com");

            logViridianConfig(clientConfig);

            LOGGER.info("Viridian configured, cluster id: "
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
        LOGGER.info("smart-routing=='{}'", clientNetworkConfig.isSmartRouting());

        return clientConfig;
    }

    /**
     * <p>Properties for SSL
     * </p>
     *
     * @param keystorePassword
     * @param truststorePassword
     * @return
     */
    private static Properties getSSLProperties(String keystorePassword, String truststorePassword) {
        Properties properties = new Properties();

        properties.setProperty("javax.net.ssl.keyStore",
                new File("./client.keystore").toURI().getPath());
        properties.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        properties.setProperty("javax.net.ssl.trustStore",
                new File("./client.truststore").toURI().getPath());
        properties.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

        return properties;
    }

    /**
     * <p>Confirms properties set correctly for Maven to pick up.
     * </p>
     *
     * @param clientConfig
     */
    private static void logViridianConfig(ClientConfig clientConfig) {
        LOGGER.info("Cluster id=='{}'", clientConfig.getClusterName());

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

        String keystorePassword = Objects.toString(clientConfig.getNetworkConfig()
                .getSSLConfig().getProperty("javax.net.ssl.keyStorePassword"));
        if (keystorePassword.length() == EXPECTED_PASSWORD_LENGTH) {
            LOGGER.info("SSL keystore password.length()=={}, ending '{}'", keystorePassword.length(),
                    keystorePassword.substring(keystorePassword.length() - 1, keystorePassword.length()));
        } else {
            LOGGER.warn("SSL keystore password.length()=={}, expected {}",
                    keystorePassword.length(),
                    EXPECTED_PASSWORD_LENGTH);
        }

        String truststorePassword = Objects.toString(clientConfig.getNetworkConfig()
                .getSSLConfig().getProperty("javax.net.ssl.trustStorePassword"));
        if (truststorePassword.length() == EXPECTED_PASSWORD_LENGTH) {
            LOGGER.info("SSL truststore password.length()=={}, ending '{}'", truststorePassword.length(),
                    truststorePassword.substring(truststorePassword.length() - 1, truststorePassword.length()));
        } else {
            LOGGER.warn("SSL truststore password.length()=={}, expected {}",
                    truststorePassword.length(),
                    EXPECTED_PASSWORD_LENGTH);
        }
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
