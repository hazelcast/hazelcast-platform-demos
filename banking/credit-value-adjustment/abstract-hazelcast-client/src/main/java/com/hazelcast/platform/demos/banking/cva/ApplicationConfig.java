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

package com.hazelcast.platform.demos.banking.cva;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Common configuration for clients of the clusters. Uses
 * properties to adjust the generic configuration loaded from files appropriate
 * to the target cluster.
 * </p>
 */
@Configuration
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

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
    public ClientConfig clientConfig(MyProperties myProperties) {
        ClientConfig clientConfig = new YamlClientConfigBuilder().build();

        clientConfig.setClusterName(myProperties.getSite().toString());

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.warn("Kubernetes configuration: service-dns: "
                    + clientNetworkConfig.getKubernetesConfig().getProperty("service-dns"));
        } else {
            clientNetworkConfig.getKubernetesConfig().setEnabled(false);

            String host = System.getProperty("hazelcast.local.publicAddress", "127.0.0.1");
            int port = MyUtils.getLocalhostBasePort(myProperties.getSite());

            List<String> memberList = List.of(host + ":" + port,
                    host + ":" + (port + 1), host + (port + 2));
            clientNetworkConfig.setAddresses(memberList);

            LOGGER.warn("Non-Kubernetes configuration: member-list: "
                    + clientNetworkConfig.getAddresses());
        }

        return clientConfig;
    }

    /**
     * <p>Create a Jet instance that is a client of the
     * cluster specified in the "{@code clientConfig}"
     * and expose as a "{@code @Bean}"</p>
     *
     * @param clientConfig created above
     * @return A Jet client
     */
    @Bean
    public JetInstance jetInstance(ClientConfig clientConfig) {
        return Jet.newJetClient(clientConfig);
    }

    /**
     * <p>Clients that don't use Jet features may prefer to
     * autowire a "{@code HazelcastInstance}" to make this clear.
     * Expose the contained Hazelcast instance as a "{@code @Bean}".
     *
     * @param jetInstance created above
     * @return The Hazelcast client that the Jet client extends.
     */
    @Bean
    public HazelcastInstance hazelcastInstance(JetInstance jetInstance) {
        return jetInstance.getHazelcastInstance();
    }
}
