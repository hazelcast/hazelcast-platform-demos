/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.ClasspathYamlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.DataPersistenceConfig;
import com.hazelcast.config.DiscoveryConfig;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.DiskTierConfig;
import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.LocalDeviceConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MemoryTierConfig;
import com.hazelcast.config.PersistenceConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.TieredStoreConfig;
import com.hazelcast.config.WanReplicationConfig;
import com.hazelcast.config.WanReplicationRef;
import com.hazelcast.memory.Capacity;
import com.hazelcast.memory.MemoryUnit;

/**
 * <p>Non-default configuration for Jet, to allow this example to run in Kubernetes (by default),
 * in Docker or as a stand-alone Java.
 * </p>
 */
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final int MERKLE_TREE_DEPTH = 2;

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
    public static Config buildConfig(Properties properties) throws Exception {
        Config config = new ClasspathYamlConfig("hazelcast.yml");

        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getAutoDetectionConfig().setEnabled(false);
        joinConfig.getMulticastConfig().setEnabled(false);

        LOGGER.info("Runtime.getRuntime().availableProcessors()=={}", Runtime.getRuntime().availableProcessors());

        boolean isKubernetes = System.getProperty("my.kubernetes.enabled", "").equals("true");
        if (isKubernetes) {
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

        // If using Enterprise
        if (config.getNativeMemoryConfig().isEnabled()) {
            TransactionMonitorFlavor transactionMonitorFlavor =
                    MyUtils.getTransactionMonitorFlavor(properties);
            addPersistentStore(config, transactionMonitorFlavor);
            addTieredStore(config, transactionMonitorFlavor);
            addUserCodeNamespace(config);
            // Might not have enough members if not running in cloud
            if (isKubernetes) {
                addCPSubsystem(config);
            }
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
    public static void addWan(Config config, Properties properties) throws Exception {
        if (!System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("WAN deactivated as not Kubernetes");
            config.getWanReplicationConfigs().clear();
            return;
        }
        TransactionMonitorFlavor transactionMonitorFlavor = MyUtils.getTransactionMonitorFlavor(properties);
        LOGGER.info("TransactionMonitorFlavor=='{}'", transactionMonitorFlavor);

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

            try {
                MyWANDiscoveryStrategyFactory myWANDiscoveryStrategyFactory
                = new MyWANDiscoveryStrategyFactory(config.getClusterName(), properties);

                for (DiscoveryStrategyConfig discoveryStrategyConfig : discoveryConfig.getDiscoveryStrategyConfigs()) {
                    discoveryStrategyConfig.setDiscoveryStrategyFactory(myWANDiscoveryStrategyFactory);
                }
            } catch (Exception e) {
                LOGGER.error("addWan()", e);
            }
        });

        List<String> wanMapNames = new ArrayList<>();
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            wanMapNames.addAll(MyConstants.WAN_IMAP_NAMES_ECOMMERCE);
            break;
        case PAYMENTS:
            wanMapNames.addAll(MyConstants.WAN_IMAP_NAMES_PAYMENTS);
            break;
        case TRADE:
        default:
            wanMapNames.addAll(MyConstants.WAN_IMAP_NAMES_TRADE);
            break;
        }
        for (String mapName : wanMapNames) {
            WanReplicationRef wanReplicationRef = new WanReplicationRef();
            wanReplicationRef.setName(publisherName);

            MapConfig mapConfig = new MapConfig();
            mapConfig.setName(mapName);
            mapConfig.getMerkleTreeConfig().setDepth(MERKLE_TREE_DEPTH).setEnabled(true);
            mapConfig.setWanReplicationRef(wanReplicationRef);

            config.addMapConfig(mapConfig);
        }
    }

    /**
     * <p>Add tiered store configuration.
     * </p>
     * <p>Watch for no clash with {@link TransactionMonitorIdempotentInitialization}. In this method
     * we add configuration for the "{@code transactions}" map, where in the other class
     * we amend the map itself.
     * </p>
     *
     * @param config
     * @throws Exception if native memory specification not a number
     */
    private static void addTieredStore(Config config, TransactionMonitorFlavor transactionMonitorFlavor) throws Exception {
        Path path = Paths.get("/" + MyConstants.STORE_BASE_DIR_PREFIX + "/" + config.getClusterName()
            + "/" + MyConstants.TIERED_STORE_SUFFIX);
        LOGGER.info("Adding Tiered Storage, {}", path.toString());

        String nativeMemoryProperty = "my.native.megabytes";
        String nativeMegabytesSpec = System.getProperty(nativeMemoryProperty, "");
        try {
            long nativeMegabytes = Long.parseLong(nativeMegabytesSpec);
            Capacity nativeCapacity = new Capacity(nativeMegabytes, MemoryUnit.MEGABYTES);
            config.getNativeMemoryConfig().setCapacity(nativeCapacity);
        } catch (NumberFormatException nfe) {
            String message = String.format("Property '%s', parse '%s'", nativeMemoryProperty, nativeMegabytesSpec);
            throw new RuntimeException(message, nfe);
        }
        LOGGER.info("'{}'=='{}', native memory to: {}",
                    nativeMemoryProperty, nativeMegabytesSpec, config.getNativeMemoryConfig().getCapacity());

        LocalDeviceConfig localDeviceConfig = new LocalDeviceConfig();
        localDeviceConfig.setName(MyConstants.TIERED_STORE_DEVICE_NAME);
        localDeviceConfig.setBaseDir(path.toFile());
        Capacity diskCapacity = new Capacity(MyConstants.TIERED_STORE_DISK_CAPACITY_GB, MemoryUnit.GIGABYTES);
        localDeviceConfig.setCapacity(diskCapacity);
        LOGGER.trace("{}", localDeviceConfig);
        config.getDeviceConfigs().put(localDeviceConfig.getName(), localDeviceConfig);

        // Same config for all selected maps
        TieredStoreConfig tieredStoreConfig = getTieredStoreConfig(localDeviceConfig);

        List<String> tieredStoreMapNames;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            tieredStoreMapNames = MyConstants.TIERED_STORE_IMAP_NAMES_ECOMMERCE;
            break;
        case PAYMENTS:
            tieredStoreMapNames = MyConstants.TIERED_STORE_IMAP_NAMES_PAYMENTS;
            break;
        case TRADE:
        default:
            tieredStoreMapNames = MyConstants.TIERED_STORE_IMAP_NAMES_TRADE;
            break;
        }

        // Augment map config
        for (String mapName : tieredStoreMapNames) {
            // "getMapConfig()" creates if not present
            MapConfig mapConfig = config.getMapConfig(mapName);
            LOGGER.debug("Setting map '{}' for TieredStore", mapConfig.getName());
            mapConfig.setInMemoryFormat(InMemoryFormat.NATIVE);
            mapConfig.setTieredStoreConfig(tieredStoreConfig);
            mapConfig.setEventJournalConfig(new EventJournalConfig().setEnabled(true));
        }
    }

    /**
     * <p>Configuration for tiered storage.
     * Memory has a size, disk has too but we can't measure it from Java.
     * </p>
     *
     * @param localDeviceConfig
     * @return
     */
    private static TieredStoreConfig getTieredStoreConfig(LocalDeviceConfig localDeviceConfig) {
        Capacity memoryCapacity = new Capacity(MyConstants.TIERED_STORE_MEMORY_CAPACITY_MB, MemoryUnit.MEGABYTES);

        MemoryTierConfig memoryTierConfig = new MemoryTierConfig();
        memoryTierConfig.setCapacity(memoryCapacity);

        DiskTierConfig diskTierConfig = new DiskTierConfig();
        diskTierConfig.setEnabled(true);
        diskTierConfig.setDeviceName(localDeviceConfig.getName());

        TieredStoreConfig tieredStoreConfig = new TieredStoreConfig();
        tieredStoreConfig.setEnabled(true);
        tieredStoreConfig.setMemoryTierConfig(memoryTierConfig);
        tieredStoreConfig.setDiskTierConfig(diskTierConfig);

        LOGGER.trace("{}", tieredStoreConfig);
        return tieredStoreConfig;
    }

    /**
     * <p>Add persistent store configuration.
     * </p>
     */
    private static void addPersistentStore(Config config, TransactionMonitorFlavor transactionMonitorFlavor) {
        Path path = Paths.get("/" + MyConstants.STORE_BASE_DIR_PREFIX + "/" + config.getClusterName()
            + "/" + MyConstants.PERSISTENT_STORE_SUFFIX);
        LOGGER.info("Adding Persistent Storage, {}", path.toString());

        PersistenceConfig persistenceConfig = config.getPersistenceConfig();
        persistenceConfig.setEnabled(true);
        persistenceConfig.setBaseDir(path.toFile());

        DataPersistenceConfig dataPersistenceConfig = new DataPersistenceConfig();
        dataPersistenceConfig.setEnabled(true);

        List<String> mapNames;
        switch (transactionMonitorFlavor) {
            case ECOMMERCE:
                mapNames = MyConstants.PERSISTENT_STORE_IMAP_NAMES_ECOMMERCE;
                break;
            case PAYMENTS:
                mapNames = MyConstants.PERSISTENT_STORE_IMAP_NAMES_PAYMENTS;
                break;
            case TRADE:
            default:
                mapNames = MyConstants.PERSISTENT_STORE_IMAP_NAMES_TRADE;
                break;
        }

        for (String mapName : mapNames) {
            // "getMapConfig()" creates if not present
            MapConfig mapConfig = config.getMapConfig(mapName);
            LOGGER.debug("Setting map '{}' for PersistentStore", mapConfig.getName());
            mapConfig.setDataPersistenceConfig(dataPersistenceConfig);
        }
    }

    /**
     * <p>Activate the CP Subsystem.
     * </p>
     *
     * @param config
     */
    private static void addCPSubsystem(Config config) {
        config.getCPSubsystemConfig()
        .setCPMemberCount(MyConstants.CP_MEMBER_SIZE)
        .setGroupSize(MyConstants.CP_GROUP_SIZE);
    }

    /**
     * <p>Add namespaces for selected objects. All three namespaces define an executor
     * and run different implementations, segregated by their classloaders.
     * </p>
     */
    private static void addUserCodeNamespace(Config config) {
        // Off in hazelcast-common.yml, turn on.
        config.getNamespacesConfig().setEnabled(true);
        LOGGER.info("config.getNamespacesConfig().isEnabled()=={}", config.getNamespacesConfig().isEnabled());

        // Namespace for executor classes
        ExecutorConfig executorConfig1 = new ExecutorConfig(MyConstants.EXECUTOR_NAMESPACE_1);
        executorConfig1.setUserCodeNamespace(MyConstants.USER_CODE_NAMESPACE_1);
        executorConfig1.setPoolSize(2);
        config.getExecutorConfigs().put(executorConfig1.getName(), executorConfig1);

        // Namespace for executor and map listener classes
        ExecutorConfig executorConfig2 = new ExecutorConfig(MyConstants.EXECUTOR_NAMESPACE_2);
        executorConfig2.setUserCodeNamespace(MyConstants.USER_CODE_NAMESPACE_2);
        config.getExecutorConfigs().put(executorConfig2.getName(), executorConfig2);

        // Namespace for executor queue listener classes
        ExecutorConfig executorConfig3 = new ExecutorConfig(MyConstants.EXECUTOR_NAMESPACE_3);
        executorConfig3.setUserCodeNamespace(MyConstants.USER_CODE_NAMESPACE_3);
        executorConfig3.setPoolSize(2);
        config.getExecutorConfigs().put(executorConfig3.getName(), executorConfig3);
    }

}
