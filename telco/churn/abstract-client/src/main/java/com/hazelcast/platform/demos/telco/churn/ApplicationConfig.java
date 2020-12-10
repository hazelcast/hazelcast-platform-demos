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

import java.util.List;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.config.KubernetesConfig;
import com.hazelcast.config.NetworkConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Shared configuration for Hazelcast (Java) clients, such as the Jet
 * job submitters.
 * </p>
 */
@Configuration
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final String YAML_CLIENT_CONFIG = "hazelcast-client.yml";
    private static final String XML_CLIENT_CONFIG = "hazelcast-client.xml";

    /**
     * <p>Set properties in the environment that may be used in Hazelcast
     * configuration files. This constructor runs before the
     * {@link #clientConfig} method which processes these configuration
     * files.
     * </p>
     */
    public ApplicationConfig(MyProperties myProperties) {
        System.setProperty("my.build-timestamp", myProperties.getBuildTimestamp());
        System.setProperty("my.build-userName", myProperties.getBuildUserName());
        System.setProperty("my.project", myProperties.getProject());
        System.setProperty("my.site", myProperties.getSite());
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
        // Try YAML then XML
        ClientConfig clientConfig = null;
        try {
            clientConfig = new YamlClientConfigBuilder(YAML_CLIENT_CONFIG).build();
            LOGGER.trace("Loading '{}'", YAML_CLIENT_CONFIG);
        } catch (Exception e) {
            LOGGER.trace("Cannot find '{}', trying '{}': '{}'",
                    YAML_CLIENT_CONFIG, XML_CLIENT_CONFIG, e.getMessage());
            clientConfig = new XmlClientConfigBuilder(XML_CLIENT_CONFIG).build();
        }

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            KubernetesConfig kubernetesConfig = new KubernetesConfig();

            kubernetesConfig.setEnabled(true);
            kubernetesConfig.setProperty("service-dns",
                    System.getProperty("my.project") + "-"
                    + System.getProperty("my.site") + "-"
                    + "hazelcast.default.svc.cluster.local");

            clientNetworkConfig.setKubernetesConfig(kubernetesConfig);

            LOGGER.warn("Kubernetes configuration: service-dns: "
                    + clientNetworkConfig.getKubernetesConfig().getProperty("service-dns"));
        } else {
            clientNetworkConfig.getKubernetesConfig().setEnabled(false);

            String host = System.getProperty("hazelcast.local.publicAddress", "127.0.0.1");
            if (host.indexOf(':') > 0) {
                // Ignore port if provided, since we'll use several
                host = host.substring(0, host.indexOf(":"));
            }
            int port = NetworkConfig.DEFAULT_PORT;

            List<String> memberList = List.of(host + ":" + port,
                    host + ":" + (port + 1), host + ":" + (port + 2));
            clientNetworkConfig.setAddresses(memberList);

            LOGGER.warn("Non-Kubernetes configuration: member-list: "
                    + clientNetworkConfig.getAddresses());
        }

        return clientConfig;
    }
}
