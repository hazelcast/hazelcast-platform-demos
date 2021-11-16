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
import com.hazelcast.client.config.YamlClientConfigBuilder;
import com.hazelcast.client.properties.ClientProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Hazelcast Client configuration, extends "{@code hazelcast-client.yml}".
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationConfig {

    /**
     * <p>Client configuration, for Cloud.
     * </p>
     *
     * @return
     * @throws Exception
     */
    @Bean
    public ClientConfig clientConfig() throws Exception {
        ClientConfig clientConfig = new YamlClientConfigBuilder("hazelcast-client.yml").build();

        String clusterDiscoveryToken = System.getProperty("CLUSTER_DISCOVERY_TOKEN", "");
        String clusterName = System.getProperty("CLUSTER_NAME", "");

        if (clusterDiscoveryToken.length() == 0 || clusterName.length() == 0) {
            String message = String.format("Need both CLUSTER_DISCOVERY_TOKEN and CLUSTER_NAME, got '%s' and '%s'",
                    clusterDiscoveryToken, clusterName);
            throw new RuntimeException(message);
        }

        clientConfig.setClusterName(clusterName);
        clientConfig.setProperty(ClientProperty.METRICS_ENABLED.getName(), "true");
        clientConfig.setProperty(ClientProperty.HAZELCAST_CLOUD_DISCOVERY_TOKEN.getName(),
                clusterDiscoveryToken);

        //FIXME Until PROD ready. Don't push as exposes internals.
        clientConfig.setProperty("hazelcast.client.cloud.url", "https://uat.hazelcast.cloud");
        clientConfig.setProperty("hazelcast.client.cloud.url", "https://dev.test.hazelcast.cloud");

        log.info("Cluster '{}' : Hazelcast cloud configuration",
                clientConfig.getClusterName());

        return clientConfig;
    }

}
