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

package com.hazelcast.platform.demos.banking.cva;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.hazelcast.config.DiscoveryConfig;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.WanBatchPublisherConfig;
import com.hazelcast.config.WanReplicationConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.config.YamlJetConfigBuilder;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final int DEFAULT_PARTITION_COUNT = 271;

    private final String project;
    private final Site site;
    private final Site remoteSite;

    @Autowired
    private MyWANDiscoveryStrategyFactory myDiscoveryStrategyFactory;

    static {
        LOGGER.info("Runtime.getRuntime().availableProcessors()=={}",
                Runtime.getRuntime().availableProcessors());
    }

    public ApplicationConfig(MyProperties myProperties) {
        // From Maven, application.yml, wouldn't be in environment by default.
        this.project = myProperties.getProject();
        this.remoteSite = myProperties.getRemoteSite();
        this.site = myProperties.getSite();
        this.checkPartitionCount(myProperties);
        System.setProperty("my.project", this.project);
        System.setProperty("my.remote.site", this.remoteSite.toString());
        System.setProperty("my.partitions", String.valueOf(myProperties.getPartitions()));
        System.setProperty("my.site", this.site.toString());

        String initSizeStr = System.getProperty("my.initSize", "");
        myProperties.setInitSize(1);
        if (initSizeStr.length() == 0) {
            LOGGER.warn("System property 'my.initSize' empty");
        } else {
            try {
                myProperties.setInitSize(Integer.parseInt(initSizeStr));
            } catch (NumberFormatException nfe) {
                LOGGER.error("System property 'my.initSize' exception '{}' '{}'",
                        initSizeStr, nfe.getMessage());
            }
        }
    }

    /**
     * <p>May not be set as provided in environment.</p>
     *
     * @param myProperties
     */
    private void checkPartitionCount(MyProperties myProperties) {
        if (myProperties.getPartitions() == 0) {
            LOGGER.warn("myProperties.getPartitions() == 0, so will use default of {}", DEFAULT_PARTITION_COUNT);
            myProperties.setPartitions(DEFAULT_PARTITION_COUNT);
        }
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
        this.logProperties(jetConfig);

        this.adjustNearCacheConfig(jetConfig.getHazelcastConfig().getMapConfigs());

        NetworkConfig networkConfig = jetConfig.getHazelcastConfig().getNetworkConfig();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            this.addWanConfig(jetConfig.getHazelcastConfig().getWanReplicationConfigs());

            LOGGER.info("Kubernetes configuration: service-dns: {}",
                    networkConfig.getJoin().getKubernetesConfig().getProperty("service-dns"));
        } else {
            this.removeWanConfig(jetConfig.getHazelcastConfig().getMapConfigs());

            networkConfig.getJoin().getKubernetesConfig().setEnabled(false);

            int port = MyUtils.getLocalhostBasePort(this.site);

            networkConfig.setPort(port);

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
     * <p>Log any set or derived properties.
     * </p>
     *
     * @param jetConfig Loaded from Yaml
     */
    private void logProperties(JetConfig jetConfig) {
        Properties properties = jetConfig.getProperties();
        for (Map.Entry propertyEntry : properties.entrySet()) {
            LOGGER.info("Property '{}'=='{}'",
                    propertyEntry.getKey(), propertyEntry.getValue());
        }
        properties = jetConfig.getHazelcastConfig().getProperties();
        for (Map.Entry propertyEntry : properties.entrySet()) {
            LOGGER.info("Property '{}'=='{}'",
                    propertyEntry.getKey(), propertyEntry.getValue());
        }
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

    /**
     * <p>We only need one publisher group, "{@code my-cva-wan-publisher-group}",
     * and publish to the other cluster, but iterate across in case other
     * groups are added later.
     * </p>
     */
    private void addWanConfig(Map<String, WanReplicationConfig> map) {
        for (Entry<String, WanReplicationConfig> publisher : map.entrySet()) {
            WanReplicationConfig wanReplicationConfig = publisher.getValue();
            for (WanBatchPublisherConfig wanBatchPublisherConfig : wanReplicationConfig.getBatchPublisherConfigs()) {
                this.addWanConfig(wanBatchPublisherConfig);
            }
        }
    }

    /**
     * <p>Site 1 publishes to site 2, and vice versa.
     * </p>
     * <p>In order to do this, each site needs to know the host:port
     * pairs for the other.
     * </p>
     * <p>This is easy on bare metal as the host:port pair is known in
     * advance. Not too difficult on Docker as the network name can
     * be set. Much harder on Kubernetes, as the IP addresses won't
     * be known til deploy time.
     * <p>
     * <p>So use a Spring "{@code @Bean}" to find the last of node
     * addresses, host:port pairs.
     * </p>
     *
     * @param wanBatchPublisherConfig
     */
    private void addWanConfig(WanBatchPublisherConfig wanBatchPublisherConfig) {
        DiscoveryConfig discoveryConfig =
                wanBatchPublisherConfig.getDiscoveryConfig();

        for (DiscoveryStrategyConfig discoveryStrategyConfig : discoveryConfig.getDiscoveryStrategyConfigs()) {
            discoveryStrategyConfig.setDiscoveryStrategyFactory(this.myDiscoveryStrategyFactory);
        }
    }


    /**
     * <p>If WAN is not applicable, as not in Kubernetes, remove it from
     * map configuration.
     * </p>
     *
     * @param mapConfigs
     */
    private void removeWanConfig(Map<String, MapConfig> mapConfigs) {
        for (MapConfig mapConfig : mapConfigs.values()) {
            if (mapConfig.getWanReplicationRef() != null) {
                mapConfig.setWanReplicationRef(null);
            }
        }
    }

}
