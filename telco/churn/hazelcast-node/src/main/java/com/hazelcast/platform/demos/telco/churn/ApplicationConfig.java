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

import com.hazelcast.config.ClasspathYamlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.platform.demos.telco.churn.mapstore.MyMapStoreFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * Non-default configuration for Jet, to allow this example to run in Kubernetes
 * (by default), in Docker or as a stand-alone Java.
 * </p>
 */
@Configuration
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    public ApplicationConfig(MyProperties myProperties) {
        // Already validated in Application.main
        myProperties.setInitSize(Integer.parseInt(System.getProperty("my.initSize")));

        LOGGER.info("Runtime.getRuntime().availableProcessors()=={}", Runtime.getRuntime().availableProcessors());
        System.setProperty("my.build-timestamp", myProperties.getBuildTimestamp());
        System.setProperty("my.build-userName", myProperties.getBuildUserName());
        System.setProperty("my.project", myProperties.getProject());
        System.setProperty("my.site", myProperties.getSite());
    }

    /**
     * <p>Extend config to wire in Spring {@code @Bean} instead
     * of normal Java class instances.
     * </p>
     */
    @Bean
    public Config config(MyMapStoreFactory myMapStoreFactory) {
        Config config = new ClasspathYamlConfig("hazelcast.yml");

        // Call data records - Cassandra
        MapConfig cdrMapConfig = new MapConfig(MyConstants.IMAP_NAME_CDR);

        MapStoreConfig cdrMapStoreConfig = new MapStoreConfig();
        cdrMapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
        cdrMapStoreConfig.setFactoryImplementation(myMapStoreFactory);
        cdrMapConfig.setMapStoreConfig(cdrMapStoreConfig);

        //config.getMapConfigs().put(cdrMapConfig.getName(), cdrMapConfig);
        //FIXME
        LOGGER.error("FIXME don't use {} yet", cdrMapConfig.getClass().getName());

        // Customer records - Mongo
        MapConfig customerMapConfig = new MapConfig(MyConstants.IMAP_NAME_CUSTOMER);

        MapStoreConfig customerMapStoreConfig = new MapStoreConfig();
        customerMapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
        customerMapStoreConfig.setFactoryImplementation(myMapStoreFactory);
        customerMapConfig.setMapStoreConfig(customerMapStoreConfig);

        //config.getMapConfigs().put(customerMapConfig.getName(), customerMapConfig);
        //FIXME
        LOGGER.error("FIXME don't use {} yet", customerMapConfig.getClass().getName());

        // Tariff records - MySql
        MapConfig tariffMapConfig = new MapConfig(MyConstants.IMAP_NAME_TARIFF);

        MapStoreConfig tariffMapStoreConfig = new MapStoreConfig();
        tariffMapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
        tariffMapStoreConfig.setFactoryImplementation(myMapStoreFactory);
        tariffMapConfig.setMapStoreConfig(tariffMapStoreConfig);

        config.getMapConfigs().put(tariffMapConfig.getName(), tariffMapConfig);

        return config;
    }

    /**
     * <p>
     * Create the configuration for Jet using "{@code *.yml}" files found on the
     * classpath. On a <i>cloud-first</i> approach, these are configured for
     * Kubernetes.
     * </p>
     * <p>
     * If the "{@code my.kubernetes.enabled}" indicates we are not in Kubernetes,
     * change the configuration to use either localhost or the provided network.
     * </p>
     * <p>
     * See <b>README.md</b> and "{@code src/main/scripts}" in the project top-level.
     * </p>
     */
    @Bean
    public JetConfig jetConfig(Config config) {
        JetConfig jetConfig = new JetConfig().setHazelcastConfig(config);

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
            host = host.replaceAll("5703", "5701").replaceAll("5702", "5701");
            tcpIpConfig.setMembers(List.of(host));

            networkConfig.getJoin().setTcpIpConfig(tcpIpConfig);

            LOGGER.info("Non-Kubernetes configuration: member-list: {}", tcpIpConfig.getMembers());
        }

        return jetConfig;
    }

    /**
     * <p>
     * On a 1-node cluster, near-caching normally has no effect. But often that will
     * be how this demo is run, so force near-caching on to make the statistics
     * correct.
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
