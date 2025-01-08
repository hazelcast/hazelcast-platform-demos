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

package com.hazelcast.platform.demos.retail.clickstream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientFailoverConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.YamlClientFailoverConfigBuilder;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Configure a failover client, it will try the first cluster
 * then the second.
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationConfig {

    /**
     * <p>Configure for Docker or Kubernetes
     * </p>
     */
    @Bean
    public HazelcastInstance hazelcastInstance() throws Exception {
        ClientFailoverConfig clientFailoverConfig =
                new YamlClientFailoverConfigBuilder("hazelcast-client-failover.yml").build();

        for (ClientConfig clientConfig : clientFailoverConfig.getClientConfigs()) {
            String clusterName = clientConfig.getClusterName();
            ClientNetworkConfig clientNetworkConfig = clientConfig.getNetworkConfig();
            if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
                this.setKubernetesNetworking(clientNetworkConfig, clusterName);

                clientNetworkConfig.getKubernetesConfig()
                .getProperties()
                .keySet()
                .forEach(key -> {
                    log.info("Cluster '{}' : Kubernetes configuration: {}: {}",
                            clusterName,
                            key,
                            clientNetworkConfig.getKubernetesConfig().getProperty(key));
                });

            } else {
                this.setDockerNetworking(clientNetworkConfig, clusterName);

                log.info("Cluster '{}' : Non-Kubernetes configuration: addresses: {}",
                        clusterName,
                        clientNetworkConfig.getAddresses());
            }
        }

        return HazelcastClient.newHazelcastFailoverClient(clientFailoverConfig);
    }

    /**
     * <p>Kubernetes DNS
     * </p>
     */
    private void setKubernetesNetworking(ClientNetworkConfig clientNetworkConfig, String clusterName) {
        String cluster1Name = System.getProperty("CLUSTER1_NAME");
        String cluster1Addresses = System.getProperty("CLUSTER1_ADDRESSLIST");
        String cluster2Addresses = System.getProperty("CLUSTER2_ADDRESSLIST");

        clientNetworkConfig.getKubernetesConfig().setEnabled(true);
        if (clusterName.equals(cluster1Name)) {
            clientNetworkConfig.getKubernetesConfig()
                .setProperty(MyConstants.KUBERNETES_PROPERTY_SERVICE_DNS, cluster1Addresses);
        } else {
            clientNetworkConfig.getKubernetesConfig()
                .setProperty(MyConstants.KUBERNETES_PROPERTY_SERVICE_DNS, cluster2Addresses);
        }

    }

    /**
     * <p>Docker all on same host.
     * </p>
     */
    private void setDockerNetworking(ClientNetworkConfig clientNetworkConfig, String clusterName) {

        String cluster1Name = System.getProperty("CLUSTER1_NAME");
        String dockerHost = System.getProperty("HOST_IP", "");
        String cluster1Ports = System.getProperty("CLUSTER1_PORTLIST");
        String cluster2Ports = System.getProperty("CLUSTER2_PORTLIST");
        String cluster1Members = dockerHost + ":" + cluster1Ports.replaceAll(",", "," + dockerHost + ":");
        String cluster2Members = dockerHost + ":" + cluster2Ports.replaceAll(",", "," + dockerHost + ":");

        if (clusterName.equals(cluster1Name)) {
            for (String address : cluster1Members.split(",")) {
                clientNetworkConfig.addAddress(address);
            }
        } else {
            for (String address : cluster2Members.split(",")) {
                clientNetworkConfig.addAddress(address);
            }
        }
    }

}
