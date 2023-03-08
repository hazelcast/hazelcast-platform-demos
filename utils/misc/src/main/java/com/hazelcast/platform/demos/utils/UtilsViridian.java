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

package com.hazelcast.platform.demos.utils;

import java.io.File;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.SSLConfig;

/**
 * <p>Utilities for working with Viridian
 * </p>
 */
public class UtilsViridian {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsViridian.class);

    // Docker images copies keystore/truststore into working directory
    private static final String DOCKER_KEYSTORE_TRUSTSTORE_DIRECTORY = ".";
    private static final int EXPECTED_PASSWORD_LENGTH = 11;
    private static final int EXPECTED_TOKEN_LENGTH = 50;

    /**
     * <p>Configure for Viridian.
     * </p>
     */
    public static void configure(ClientConfig clientConfig, String clusterId, String discoveryToken, String keyPassword)
        throws Exception {
        clientConfig.setClusterName(clusterId);

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();
        clientNetworkConfig.getAutoDetectionConfig().setEnabled(false);
        clientNetworkConfig.getKubernetesConfig().setEnabled(false);
        clientNetworkConfig.getCloudConfig().setEnabled(true);
        clientNetworkConfig.getCloudConfig().setDiscoveryToken(discoveryToken);

        Properties sslProperties = getSSLProperties(keyPassword, DOCKER_KEYSTORE_TRUSTSTORE_DIRECTORY);

        SSLConfig sslConfig = new SSLConfig();
        sslConfig.setEnabled(true).setProperties(sslProperties);

        clientNetworkConfig.setSSLConfig(sslConfig);

        clientConfig.setProperty("hazelcast.client.cloud.url", "https://api.viridian.hazelcast.com");

        logViridianConfig(clientConfig);
    }

    /**
     * <p>Properties for SSL
     * </p>
     *
     * @param keyPassword - same for both KeyStore and TrustStore
     * @param directory - where the KeyStore/TrustStore files are found, names are preset
     * @return
     */
    public static Properties getSSLProperties(String keyPassword, String directory) {
        Properties properties = new Properties();

        properties.setProperty("javax.net.ssl.keyStore",
                new File(directory + "/client.keystore").toURI().getPath());
        properties.setProperty("javax.net.ssl.keyStorePassword", keyPassword);
        properties.setProperty("javax.net.ssl.trustStore",
                new File(directory + "/client.truststore").toURI().getPath());
        properties.setProperty("javax.net.ssl.trustStorePassword", keyPassword);

        return properties;
    }

    /**
     * <p>Confirms properties set correctly for Maven to pick up.
     * </p>
     *
     * @param clientConfig
     */
    private static void logViridianConfig(ClientConfig clientConfig) throws Exception {
        LOGGER.info("Cluster id=='{}'", clientConfig.getClusterName());

        String token = Objects.toString(clientConfig.getNetworkConfig()
                .getCloudConfig().getDiscoveryToken());
        if (token.length() == EXPECTED_TOKEN_LENGTH) {
            LOGGER.info("Discovery token.length()=={}, ending '{}'", token.length(),
                    token.substring(token.length() - 1, token.length()));
        } else {
            String message =
                    String.format("Discovery token.length()==%d, expected %d",
                    token.length(),
                    EXPECTED_TOKEN_LENGTH);
            throw new RuntimeException(message);
        }

        String keystorePassword = Objects.toString(clientConfig.getNetworkConfig()
                .getSSLConfig().getProperty("javax.net.ssl.keyStorePassword"));
        if (keystorePassword.length() == EXPECTED_PASSWORD_LENGTH) {
            LOGGER.info("SSL keystore password.length()=={}, ending '{}'", keystorePassword.length(),
                    keystorePassword.substring(keystorePassword.length() - 1, keystorePassword.length()));
        } else {
            String message =
                    String.format("SSL keystore password.length()==%d, expected %d",
                    keystorePassword.length(),
                    EXPECTED_PASSWORD_LENGTH);
            throw new RuntimeException(message);
        }

        String truststorePassword = Objects.toString(clientConfig.getNetworkConfig()
                .getSSLConfig().getProperty("javax.net.ssl.trustStorePassword"));
        if (truststorePassword.length() == EXPECTED_PASSWORD_LENGTH) {
            LOGGER.info("SSL truststore password.length()=={}, ending '{}'", truststorePassword.length(),
                    truststorePassword.substring(truststorePassword.length() - 1, truststorePassword.length()));
        } else {
            String message =
                    String.format("SSL truststore password.length()==%d, expected %d",
                    truststorePassword.length(),
                    EXPECTED_PASSWORD_LENGTH);
            throw new RuntimeException(message);
        }
    }

}
