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

package com.hazelcast.platform.demos.banking.cva;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.config.KubernetesConfig;
import com.hazelcast.platform.demos.utils.UtilsViridian;

/**
 * <p>Common configuration for clients of the clusters. Uses
 * properties to adjust the generic configuration loaded from files appropriate
 * to the target cluster.
 * </p>
 */
@Configuration
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    private final String project;
    @Value("${hazelcast.local.publicAddress:}")
    private String publicAddress;

    public ApplicationConfig(MyProperties myProperties) {
        this.project = myProperties.getProject();
        System.setProperty("my.project", this.project);
    }

    /**
     * <p>Produce configuration for a client based on "{@code *.yml}" configuration
     * files loaded from the classpath, and amended accordingly.
     * </p>
     * <p>Amendments include the cluster name, and may include the networking.
     * </p>
     * <p>By default the assumption is this is a Kubernetes deployment, but if
     * indicated by the flag "{@code my.kubernetes.enabled}" this is replaced
     * with explicit IP addresses.
     * </p>
     */
    @Bean
    public ClientConfig clientConfig(MyProperties myProperties) throws Exception {
        ClientConfig clientConfig = new YamlClientConfigBuilder().build();

        clientConfig.addLabel(myProperties.getBuildTimestamp());

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();
        clientNetworkConfig.getAutoDetectionConfig().setEnabled(false);

        LOGGER.info("useViridian='{}'", myProperties.isUseViridian());
        boolean localhost = this.publicAddress.length() == 0;

        if (localhost && myProperties.isUseViridian()) {
            String message = "Localhost access not implemented for Viridian, keystore/truststore location"
                    + " not known, use Docker script instead";
            throw new RuntimeException(message);
        }

        if (myProperties.isUseViridian()) {
            UtilsViridian.configure(clientConfig,
                    myProperties.getViridianCluster1Id(),
                    myProperties.getViridianCluster1DiscoveryToken(),
                    myProperties.getViridianCluster1KeyPassword());

            LOGGER.info("Viridian configured, cluster id: "
                    + clientConfig.getClusterName());
        } else {
            clientConfig.setClusterName(myProperties.getSite().toString());

            if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
                KubernetesConfig kubernetesConfig = new KubernetesConfig();

                kubernetesConfig.setEnabled(true);
                kubernetesConfig.setProperty("service-dns",
                        System.getProperty("my.project") + "-"
                        + System.getProperty("my.site") + "-hazelcast.default.svc.cluster.local");

                clientNetworkConfig.setKubernetesConfig(kubernetesConfig);

                LOGGER.warn("Kubernetes configuration: service-dns: "
                        + clientNetworkConfig.getKubernetesConfig().getProperty("service-dns"));
            } else {
                clientNetworkConfig.getKubernetesConfig().setEnabled(false);

                String host;
                if (this.publicAddress.isEmpty()) {
                    LOGGER.info("this.publicAddress.isEmpty()");
                    host = System.getProperty("hazelcast.local.publicAddress", "127.0.0.1");
                } else {
                    LOGGER.info("!this.publicAddress.isEmpty()");
                    host = this.publicAddress;
                }

                int port = MyUtils.getLocalhostBasePort(myProperties.getSite());

                List<String> memberList = List.of(host + ":" + port,
                        host + ":" + (port + 1), host + ":" + (port + 2));
                clientNetworkConfig.setAddresses(memberList);

                LOGGER.warn("Non-Kubernetes configuration: member-list: "
                        + clientNetworkConfig.getAddresses());
            }
        }

        return clientConfig;
    }

    /**
     * <p>Expose the site as a String for web pages, etc.
     * </p>
     *
     * @param myProperties
     * @return
     */
    @Bean
    public String siteLowerCase(MyProperties myProperties) {
        return myProperties.getSite().toString().toLowerCase(Locale.ROOT);
    }
}
