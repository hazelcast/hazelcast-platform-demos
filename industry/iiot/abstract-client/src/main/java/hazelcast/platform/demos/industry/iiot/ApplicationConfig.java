/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.industry.iiot;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Hazelcast Client configuration, extends "{@code hazelcast-client.yml}",
 * mainly networking for the runtime environment.
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationConfig {

    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * <p>Client configuration, for Docker, all on same host network.
     * </p>
     *
     * @return
     * @throws Exception
     */
    @Bean
    public ClientConfig clientConfig(MyProperties myProprties) throws Exception {
        ClientConfig clientConfig = new YamlClientConfigBuilder("hazelcast-client.yml").build();
        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();

        clientConfig.setInstanceName(this.springApplicationName);
        this.validateNames(clientConfig);
        clientConfig.addLabel(myProprties.getBuildUserName());
        clientConfig.addLabel(myProprties.getBuildTimestamp());

        // If HOST_IP is set, set for all running in Docker on same host.
        String dockerHost = System.getProperty("HOST_IP", "");

        if (dockerHost.length() == 0) {
            log.info("Cluster '{}' : Hazelcast cloud configuration",
                    clientConfig.getClusterName());

            String discoveryToken = clientNetworkConfig.getCloudConfig().getDiscoveryToken();
            if (discoveryToken.length() == 0) {
                String message = String.format("Incorrect cluster discovery token '%s',"
                        + " check pom.xml for ${hz.cloud.cluster1.discovery.token}",
                        Objects.toString(discoveryToken));
                throw new RuntimeException(message);
            }
            /*FIXME Until PROD ready. Don't push as exposes internals.
            clientConfig.setProperty("hazelcast.client.cloud.url", "https://uat.hazelcast.cloud");
    */
            clientConfig.setProperty("hazelcast.client.cloud.url", "https://dev.test.hazelcast.cloud");
        } else {
            log.info("Cluster '{}' : Hazelcast local configuration",
                    clientConfig.getClusterName());

            clientNetworkConfig.getCloudConfig().setEnabled(false);

            clientNetworkConfig.addAddress(dockerHost + ":5701");
            clientNetworkConfig.addAddress(dockerHost + ":5702");
            clientNetworkConfig.addAddress(dockerHost + ":5703");

            log.info("Cluster '{}' : Docker discovery configuration: addresses: {}",
                    clientConfig.getClusterName(),
                    clientNetworkConfig.getAddresses());
        }

        return clientConfig;
    }

    private void validateNames(ClientConfig clientConfig) throws Exception {
        if (clientConfig.getClusterName() == null || clientConfig.getClusterName().length() == 0) {
            String message = String.format("Incorrect cluster name '%s', check pom.xml for ${hz.cloud.cluster1.name}",
                    Objects.toString(clientConfig.getClusterName()));
            throw new RuntimeException(message);
        }

        if (clientConfig.getInstanceName() == null || clientConfig.getInstanceName().length() == 0) {
            String message = String.format("Incorrect instance name '%s',"
                    + " check application.yml for ${spring.application.name.name}",
                    Objects.toString(clientConfig.getInstanceName()));
            throw new RuntimeException(message);
        }
    }

}
