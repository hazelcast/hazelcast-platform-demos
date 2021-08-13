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

package com.hazelcast.platform.demos.travel.booking;

import java.util.List;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.config.NetworkConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <pOverride configuration if not running in Kubernetes.
 * </p>
 */
@Configuration
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    /**
     * <p>If not Kubernetes, assume localhost or Docker containers running on same
     * host.
     * </p>
     */
    @Bean
    public ClientConfig clientConfig() throws Exception {
        ClientConfig clientConfig = new YamlClientConfigBuilder("hazelcast-client.yml").build();

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("Kubernetes configuration: service-dns: "
                    + clientNetworkConfig.getAddresses());
        } else {
            clientNetworkConfig.getKubernetesConfig().setEnabled(false);

            String host = System.getProperty("HOST_IP", "127.0.0.1");
            if (host.indexOf(':') > 0) {
                // Ignore port if provided, since we'll use several
                host = host.substring(0, host.indexOf(":"));
            }
            int port = NetworkConfig.DEFAULT_PORT;

            List<String> memberList = List.of(host + ":" + port,
                    host + ":" + (port + 1), host + ":" + (port + 2));
            clientNetworkConfig.setAddresses(memberList);

            LOGGER.info("Non-Kubernetes configuration: member-list: "
                    + clientNetworkConfig.getAddresses());
        }

        return clientConfig;
    }
}
