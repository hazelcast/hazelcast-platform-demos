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

package hazelcast.platform.demos.banking.trademonitor;

import java.util.Arrays;
import java.util.Properties;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.ClasspathYamlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryConfig;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.WanReplicationConfig;
import com.hazelcast.config.WanReplicationRef;

/**
 * <p>Non-default configuration for Jet, to allow this example to run in Kubernetes (by default),
 * in Docker or as a stand-alone Java.
 * </p>
 */
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    /**
     * <p>Create the configuration for Jet, using any default files found on the classpath.
     * These are "{@code hazelcast-jet.yml}" for Jet, and "{@code hazelcast.yml}" for the
     * IMDG instance embedded in Jet. You can also do this with XML instead of YAML if that
     * is your preference.
     * </p>
     * <p>The default behaviour, specified in "{@code hazelcast.yml}" is for Kubernetes,
     * so to run outside Kubernetes set the environment variable "{@code my.kubernetes.enabled}"
     * to "{@code false}" and the configuration loaded from the YAML file will be amended
     * for TCP based discovery, which works on Docker and normal hosts. Refer to <b>README.md</b>
     * for more details.
     * </p>
     */
    public static Config buildConfig(Properties properties) {
        Config config = new ClasspathYamlConfig("hazelcast.yml");

        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getAutoDetectionConfig().setEnabled(false);
        joinConfig.getMulticastConfig().setEnabled(false);

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("Kubernetes configuration: service-dns: {}",
                    joinConfig.getKubernetesConfig().getProperty("service-dns"));
        } else {
            joinConfig.getKubernetesConfig().setEnabled(false);

            TcpIpConfig tcpIpConfig = new TcpIpConfig();
            tcpIpConfig.setEnabled(true);
            if (System.getProperty("MY_HAZELCAST_SERVERS", "").length() != 0) {
                tcpIpConfig.setMembers(Arrays.asList(System.getProperty("MY_HAZELCAST_SERVERS").split(",")));
            } else {
                String host = System.getProperty("hazelcast.local.publicAddress", "127.0.0.1");
                host = host.replaceAll("5703", "5701").replaceAll("5702", "5701");
                tcpIpConfig.setMembers(Arrays.asList(host.split(",")));
            }

            joinConfig.setTcpIpConfig(tcpIpConfig);

            LOGGER.info("Non-Kubernetes configuration: member-list: {}", tcpIpConfig.getMembers());
            LOGGER.info("Non-Kubernetes configuration: use port: {}", config.getNetworkConfig().getPort());
        }

        // If using Enterprise
        if (!config.getWanReplicationConfigs().isEmpty()) {
            addWan(config, properties);
        }

        return config;
    }

    /**
     * <p>Enable selected maps for WAN, and add endpoint configuration.
     * </p>
     *
     * @param config To amend in-situ
     * @param properties From "application.properties"
     */
    public static void addWan(Config config, Properties properties) {
        if (!System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("WAN deactivated as not Kubernetes");
            config.getWanReplicationConfigs().clear();
            return;
        }

        // Assume only one
        Entry<String, WanReplicationConfig> entry =
                config.getWanReplicationConfigs().entrySet().iterator().next();

        String publisherName = entry.getKey();
        WanReplicationConfig wanReplicationConfig = entry.getValue();

        wanReplicationConfig
        .getBatchPublisherConfigs()
        .forEach(wanBatchPublisherConfig -> {
            LOGGER.info("WAN Publishing to cluster: '{}'", wanBatchPublisherConfig.getClusterName());
            DiscoveryConfig discoveryConfig = wanBatchPublisherConfig.getDiscoveryConfig();

            MyWANDiscoveryStrategyFactory myWANDiscoveryStrategyFactory
                = new MyWANDiscoveryStrategyFactory(config.getClusterName(), properties);

            for (DiscoveryStrategyConfig discoveryStrategyConfig : discoveryConfig.getDiscoveryStrategyConfigs()) {
                discoveryStrategyConfig.setDiscoveryStrategyFactory(myWANDiscoveryStrategyFactory);
            }
        });

        for (String mapName : MyConstants.WAN_IMAP_NAMES) {
            WanReplicationRef wanReplicationRef = new WanReplicationRef();
            wanReplicationRef.setName(publisherName);

            MapConfig mapConfig = new MapConfig();
            mapConfig.setName(mapName);
            mapConfig.setWanReplicationRef(wanReplicationRef);

            config.addMapConfig(mapConfig);
        }
    }
}
