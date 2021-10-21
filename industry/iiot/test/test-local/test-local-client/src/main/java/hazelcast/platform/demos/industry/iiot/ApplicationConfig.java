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

package hazelcast.platform.demos.industry.iiot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientConfigBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Hazelcast Client configuration, extends "{@code hazelcast-client.yml}".
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationConfig {

    /**
     * <p>Client configuration, for Docker, all on same host network.
     * </p>
     *
     * @return
     * @throws Exception
     */
    @Bean
    public ClientConfig clientConfig() throws Exception {
        ClientConfig clientConfig = new YamlClientConfigBuilder("hazelcast-client.yml").build();

        String dockerHost = System.getProperty("HOST_IP", "");

        ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();
        clientNetworkConfig.addAddress(dockerHost + ":5701");
        clientNetworkConfig.addAddress(dockerHost + ":5702");
        clientNetworkConfig.addAddress(dockerHost + ":5703");

        log.info("Cluster '{}' : Non-Kubernetes configuration: addresses: {}",
                clientConfig.getClusterName(),
                clientNetworkConfig.getAddresses());

        return clientConfig;
    }

}
