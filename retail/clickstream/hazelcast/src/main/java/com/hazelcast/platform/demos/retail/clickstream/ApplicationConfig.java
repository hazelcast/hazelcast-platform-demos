/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import java.io.File;
import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.ClasspathYamlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.WanBatchPublisherConfig;
import com.hazelcast.config.WanReplicationConfig;
import com.hazelcast.config.WanReplicationRef;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Take config from "{@code hazelcast.yml}" and adjust to need.
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationConfig {

    /**
     * <p>For Docker, assume all nodes run on same host IP.
     * This is found in "{@code hazelcast.local.publicAddress}" along with
     * the external port.
     * </p>
     */
    @Bean
    public Config config(MyMapStoreFactory myMapStoreFactory) {
        Config config = new ClasspathYamlConfig("hazelcast.yml");

        String nodeName = System.getProperty("NODE_NAME", "");
        if (nodeName.length() > 0) {
            config.setInstanceName(nodeName);
        }

        // Used to derive which is local and remote
        String cluster1Name = System.getProperty("CLUSTER1_NAME");
        String cluster2Name = System.getProperty("CLUSTER2_NAME");
        String remoteClusterName = config.getClusterName().equals(cluster1Name)
                ? cluster2Name : cluster1Name;

        // Stash cluster name for lookup by failover client
        config.getMemberAttributeConfig().getAttributes()
            .put(MyConstants.CONFIG_MAP_KEY_CLUSTER_NAME, config.getClusterName());

        NetworkConfig networkConfig = config.getNetworkConfig();
        String remoteClusterMembers;
        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {

            remoteClusterMembers =
                this.setKubernetesNetworking(networkConfig, config.getClusterName(), cluster1Name);

            networkConfig.getJoin().getKubernetesConfig()
            .getProperties()
            .keySet()
            .forEach(key -> {
                log.info("Kubernetes configuration: {}: {}",
                        key,
                        networkConfig.getJoin().getKubernetesConfig().getProperty(key));
            });

        } else {

            remoteClusterMembers =
                this.setDockerNetworking(networkConfig, config.getClusterName(), cluster1Name);

            log.info("Non-Kubernetes configuration: member-list: {}",
                    networkConfig.getJoin().getTcpIpConfig().getMembers());
        }

        log.info("Cluster '{}', set member addresses: {} with WAN to cluster '{}' at: {}",
                config.getClusterName(),
                config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers(),
                remoteClusterName, remoteClusterMembers);

        // Can have more than 1 publisher, each to more than 1 target cluster
        String publisher = "publisher-1-from-" + config.getClusterName();
        this.addWan(config, publisher, remoteClusterName, remoteClusterMembers);

        // Select disk directory for saving data
        //FIXME https://github.com/hazelcast/hazelcast/issues/22578 https://github.com/hazelcast/hazelcast/issues/22609
        if ("".equals(System.getProperty("user.name"))) {
        this.addPersistence(config);
        }

        // Non-default configuration for selected maps.
        this.setMapsForJournalWanAndPersistence(config, myMapStoreFactory, publisher, true);

        return config;
    }

    /**
     * <p>For Kubernetes, use the DNS name for the "{@code StatefulSet}".
     * </p>
     * <p>This code modifies an input and returns an output.
     * </p>
     *
     * @param networkConfig In the main config.
     * @param clusterName From the main config.
     * @param cluster1Name To compare against.
     * @param Kubernetes address of the remote cluster
     */
    private String setKubernetesNetworking(NetworkConfig networkConfig, String clusterName, String cluster1Name) {
        networkConfig.getJoin().getKubernetesConfig().setEnabled(true);

        String cluster1Addresses = System.getProperty("CLUSTER1_ADDRESSLIST");
        String cluster2Addresses = System.getProperty("CLUSTER2_ADDRESSLIST");

        if (clusterName.equals(cluster1Name)) {
            networkConfig.getJoin().getKubernetesConfig()
                .setProperty(MyConstants.KUBERNETES_PROPERTY_SERVICE_DNS, cluster1Addresses);
            return cluster2Addresses;
        } else {
            networkConfig.getJoin().getKubernetesConfig()
                .setProperty(MyConstants.KUBERNETES_PROPERTY_SERVICE_DNS, cluster2Addresses);
            return cluster1Addresses;
        }
    }

    /**
     * <p>For Docker, use the underlying host assuming all containers on the same.
     * </p>
     * <p>This code modifies an input and returns an output.
     * </p>
     *
     * @param networkConfig In the main config.
     * @param clusterName From the main config.
     * @param cluster1Name To compare against.
     * @param DNS address of the remote cluster
     */
    private String setDockerNetworking(NetworkConfig networkConfig, String clusterName, String cluster1Name) {
        TcpIpConfig tcpIpConfig = new TcpIpConfig();
        tcpIpConfig.setEnabled(true);
        networkConfig.getJoin().setTcpIpConfig(tcpIpConfig);

        String publicAddress = System.getProperty("hazelcast.local.publicAddress", "");
        String dockerHost = publicAddress.split(":")[0];
        String cluster1Ports = System.getProperty("CLUSTER1_PORTLIST");
        String cluster2Ports = System.getProperty("CLUSTER2_PORTLIST");
        String cluster1Members = dockerHost + ":" + cluster1Ports.replaceAll(",", "," + dockerHost + ":");
        String cluster2Members = dockerHost + ":" + cluster2Ports.replaceAll(",", "," + dockerHost + ":");

        if (clusterName.equals(cluster1Name)) {
            tcpIpConfig.setMembers(Arrays.asList(cluster1Members));
            networkConfig.setPort(Integer.parseInt(cluster1Ports.split(",")[0]));
            return cluster2Members;
        } else {
            tcpIpConfig.setMembers(Arrays.asList(cluster2Members));
            networkConfig.setPort(Integer.parseInt(cluster2Ports.split(",")[0]));
            return cluster1Members;
        }
    }

    /**
     * <p>Define a publishing connection to another cluster.
     * </p>
     *
     * @param config In the main config;
     * @param publisher Name for the publishing connection
     * @param remoteClusterName A set of 1 targets
     * @param remoteClusterMembers Addresses for cluster in set of 1 target
     */
    private void addWan(Config config, String publisher, String remoteClusterName, String remoteClusterMembers) {
        // A cluster to publish to
        WanBatchPublisherConfig myWanBatchPublisherConfig = new WanBatchPublisherConfig();
        myWanBatchPublisherConfig.setClusterName(remoteClusterName);
        myWanBatchPublisherConfig.setPublisherId("to-" + remoteClusterName);
        myWanBatchPublisherConfig.setQueueCapacity(MyConstants.WAN_QUEUE_SIZE);
        myWanBatchPublisherConfig.setBatchSize(MyConstants.WAN_BATCH_SIZE);

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            myWanBatchPublisherConfig.setUseEndpointPrivateAddress(false);
            this.setKubernetesWanDiscoveryConfig(remoteClusterMembers, myWanBatchPublisherConfig);
        } else {
            // For Docker
            myWanBatchPublisherConfig.setTargetEndpoints(remoteClusterMembers);
        }

        // A group of 1 clusters to publish to
        WanReplicationConfig myWanReplicationConfig = new WanReplicationConfig();
        myWanReplicationConfig.setName(publisher);
        myWanReplicationConfig.getBatchPublisherConfigs().add(myWanBatchPublisherConfig);

        config.getWanReplicationConfigs().put(myWanReplicationConfig.getName(), myWanReplicationConfig);
    }

    /**
     * <p>This demo runs both Hazelcast clusters in the same Kubernetes cluster, so can
     * use DNS lookup.
     * </p>
     *
     * @return
     */
    private void setKubernetesWanDiscoveryConfig(String remoteClusterMembers, WanBatchPublisherConfig myWanBatchPublisherConfig) {
        DiscoveryStrategyConfig discoveryStrategyConfig =
                // Class not public, can't use HazelcastKubernetesDiscoveryStrategy.class.getName()
                new DiscoveryStrategyConfig("com.hazelcast.kubernetes.HazelcastKubernetesDiscoveryStrategy");
        discoveryStrategyConfig.addProperty(MyConstants.KUBERNETES_PROPERTY_SERVICE_DNS, remoteClusterMembers);

        myWanBatchPublisherConfig.getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);
    }

    /**
     * <p>Define directory for persistent data, saved by Hazelcast.
     * </p>
     *
     * @param config In the main config;
     */
    private void addPersistence(Config config) {
        config.getPersistenceConfig()
        .setEnabled(true)
        .setBaseDir(new File(MyConstants.PERSISTENCE_BASEDIR));
    }

    /**
     * <p>Select some maps for WAN replication, event journal (history ringbuffer),
     * Hazelcast persistence and Cassandra persistence.
     * </p>
     *
     * @param config In the main config;
     * @param myMapStoreFactory Provides map store as "{@code @Bean}"
     * @param All maps use same publisher
     * @param WAN on or off, helpful for one-way replication
     */
    private void setMapsForJournalWanAndPersistence(Config config, MyMapStoreFactory myMapStoreFactory,
            String publisher, boolean includeWan) {
        WanReplicationRef wanReplicationRef = new WanReplicationRef();
        wanReplicationRef.setName(publisher);

        EventJournalConfig eventJournalConfig = new EventJournalConfig();
        eventJournalConfig.setEnabled(true);
        eventJournalConfig.setCapacity(MyConstants.JOURNAL_CAPACITY);

        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
        mapStoreConfig.setFactoryImplementation(myMapStoreFactory);

        // Checkout is journalled
        MapConfig checkoutMapConfig = new MapConfig(MyConstants.IMAP_NAME_CHECKOUT);
        checkoutMapConfig.setEventJournalConfig(eventJournalConfig);

        // Clickstream is journalled
        MapConfig clickstreamMapConfig = new MapConfig(MyConstants.IMAP_NAME_CLICKSTREAM);
        clickstreamMapConfig.setEventJournalConfig(eventJournalConfig);

        // Heartbeat is replicated and journalled
        MapConfig heartbeatMapConfig = new MapConfig(MyConstants.IMAP_NAME_HEARTBEAT);
        heartbeatMapConfig.setEventJournalConfig(eventJournalConfig);
        if (includeWan) {
            heartbeatMapConfig.setWanReplicationRef(wanReplicationRef);
        }

        // Model selected to use, and loaded by remote side instead of WAN.
        MapConfig modelSelectionMapConfig = new MapConfig(MyConstants.IMAP_NAME_MODEL_SELECTION);
        modelSelectionMapConfig.setEventJournalConfig(eventJournalConfig);
        if (includeWan) {
            modelSelectionMapConfig.setWanReplicationRef(wanReplicationRef);
        }

        // Trained models are saved, and loaded by remote side instead of WAN.
        MapConfig modelVaultMapConfig = new MapConfig(MyConstants.IMAP_NAME_MODEL_VAULT);
        modelVaultMapConfig.setMapStoreConfig(mapStoreConfig);

        // Orders is journalled
        MapConfig orderedMapConfig = new MapConfig(MyConstants.IMAP_NAME_ORDERED);
        orderedMapConfig.setEventJournalConfig(eventJournalConfig);

        // Prediction is journalled
        MapConfig predictionMapConfig = new MapConfig(MyConstants.IMAP_NAME_PREDICTION);
        predictionMapConfig.setEventJournalConfig(eventJournalConfig);

        // Retraining metadata is shared
        MapConfig retrainingAssessmentMapConfig = new MapConfig(MyConstants.IMAP_NAME_RETRAINING_ASSESSMENT);
        if (includeWan) {
            retrainingAssessmentMapConfig.setWanReplicationRef(wanReplicationRef);
        }

        config.getMapConfigs().put(checkoutMapConfig.getName(), checkoutMapConfig);
        config.getMapConfigs().put(clickstreamMapConfig.getName(), clickstreamMapConfig);
        config.getMapConfigs().put(heartbeatMapConfig.getName(), heartbeatMapConfig);
        config.getMapConfigs().put(modelSelectionMapConfig.getName(), modelSelectionMapConfig);
        config.getMapConfigs().put(modelVaultMapConfig.getName(), modelVaultMapConfig);
        config.getMapConfigs().put(orderedMapConfig.getName(), orderedMapConfig);
        config.getMapConfigs().put(predictionMapConfig.getName(), predictionMapConfig);
        config.getMapConfigs().put(retrainingAssessmentMapConfig.getName(), retrainingAssessmentMapConfig);
    }
}
