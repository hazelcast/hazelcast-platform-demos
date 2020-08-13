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
import java.util.Map;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.config.YamlJetConfigBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Non-default configuration for Jet, to allow this example to run in Kubernetes (by default),
 * in Docker or as a stand-alone Java.
 * </p>
 */
@Configuration
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    public ApplicationConfig(MyProperties myProperties) {
        LOGGER.info("Runtime.getRuntime().availableProcessors()=={}",
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * <p>Create the configuration for Jet using "{@code *.yml}" files found on the classpath.
     * On a <i>cloud-first</i> approach, these are configured for Kubernetes.
     * </p>
     * <p>If the "{@code my.kubernetes.enabled}" indicates we are not in Kubernetes,
     * change the configuration to use either localhost or the provided network.
     * </p>
     * <p>See <b>README.md</b> and "{@code src/main/scripts}" in the project top-level.
     * </p>
     */
    @Bean
    public JetConfig jetConfig() {
        JetConfig jetConfig = new YamlJetConfigBuilder().build();

        this.adjustNearCacheConfig(jetConfig.getHazelcastConfig().getMapConfigs());

        NetworkConfig networkConfig = jetConfig.getHazelcastConfig().getNetworkConfig();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("Kubernetes configuration: service-dns: {}",
                    networkConfig.getJoin().getKubernetesConfig().getProperty("service-dns"));
        } else {
            networkConfig.getJoin().getKubernetesConfig().setEnabled(false);

            TcpIpConfig tcpIpConfig = new TcpIpConfig();
            tcpIpConfig.setEnabled(true);
            String host = System.getProperty("hazelcast.local.publicAddress", "127.0.0.1");
            tcpIpConfig.setMembers(List.of(host));

            networkConfig.getJoin().setTcpIpConfig(tcpIpConfig);

            LOGGER.info("Non-Kubernetes configuration: member-list: {}",
                    tcpIpConfig.getMembers());
        }

        return jetConfig;
    }

    /**
     * <p>On a 1-node cluster, near-caching normally has no effect. But
     * often that will be how this demo is run, so force near-caching on
     * to make the statistics correct.
     * </p>
     *
     * @param mapConfigs All map configs
     */
    private void adjustNearCacheConfig(Map<String, MapConfig> mapConfigs) {
        for (MapConfig mapConfig : mapConfigs.values()) {
            NearCacheConfig nearCacheCOnfig = mapConfig.getNearCacheConfig();
            if (nearCacheCOnfig != null) {
                nearCacheCOnfig.setCacheLocalEntries(true);
            }
        }
    }

}
