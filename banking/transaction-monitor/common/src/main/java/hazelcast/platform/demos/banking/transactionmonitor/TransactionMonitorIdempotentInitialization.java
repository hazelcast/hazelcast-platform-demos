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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.config.IndexConfig;
import com.hazelcast.config.IndexType;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.map.IMap;
import com.hazelcast.mapstore.GenericMapStore;
import com.hazelcast.platform.demos.utils.UtilsConstants;
import com.hazelcast.platform.demos.utils.UtilsFormatter;
import com.hazelcast.platform.demos.utils.UtilsJobs;
import com.hazelcast.platform.demos.utils.UtilsSlack;
import com.hazelcast.platform.demos.utils.UtilsSlackSQLJob;
import com.hazelcast.platform.demos.utils.UtilsSlackSink;

/**
 * <p>May be invoked from clientside or serverside to ensure serverside ready.
 * </p>
 * <p>Has to be idempotent, so a client can call at start-up without
 * having to test if another client has already run it.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodCount")
public class TransactionMonitorIdempotentInitialization {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMonitorIdempotentInitialization.class);
    private static final Logger LOGGER_TO_IMAP =
            IMapLoggerFactory.getLogger(TransactionMonitorIdempotentInitialization.class);
    private static final int POS4 = 4;
    private static final int POS6 = 6;

    /**
     * <p>Ensure objects have the necessary configuration before
     * accessing, as the access creates them. Some configuration
     * such as journals must be active from the outset, other
     * such as indexes can be added while running.
     * </p>
     * <p>Access the {@link com.hazelcast.map.IMap} and other objects
     * that are used by the example. This will create them on first
     * access, so ensuring all are visible from the outset.
     * </p>
     */
    public static boolean createNeededObjects(HazelcastInstance hazelcastInstance,
            Properties properties, String ourProjectProvenance,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean localhost, boolean useViridian) {
        // Capture what was present before
        Set<String> existingIMapNames = hazelcastInstance.getDistributedObjects().stream()
                .filter(distributedObject -> distributedObject instanceof IMap)
                .map(distributedObject -> distributedObject.getName())
                .filter(name -> !name.startsWith("__")).collect(Collectors.toCollection(TreeSet::new));

        LOGGER.info("Existing maps: {}", existingIMapNames);

        // Add journals and map stores to maps before they are created
        boolean ok = dynamicMapConfig(hazelcastInstance, existingIMapNames,
                properties, ourProjectProvenance, localhost, useViridian, transactionMonitorFlavor);

        // Accessing non-existing maps does not return any failures
        List<String> iMapNames;
        switch (transactionMonitorFlavor) {
            case ECOMMERCE:
                iMapNames = MyConstants.IMAP_NAMES_ECOMMERCE;
                break;
            case PAYMENTS:
                iMapNames = MyConstants.IMAP_NAMES_PAYMENTS;
                break;
            case TRADE:
            default:
                iMapNames = MyConstants.IMAP_NAMES_TRADES;
                break;
        }
        for (String iMapName :iMapNames) {
            if (!existingIMapNames.contains(iMapName)) {
                hazelcastInstance.getMap(iMapName);
            }
        }

        // Add index to maps after they are created, if created in this method's run.
        if (ok) {
            ok = defineIndexes(hazelcastInstance, existingIMapNames, transactionMonitorFlavor, useViridian);
        }

        return ok;
    }

    /**
     * <p>Add journal and map store configuration to maps that need them. Equivalent to:
     * <pre>
     *     'alerts*':
     *       event-journal:
     *         enabled: true
     *       map-store:
     *         enabled: true
     *         class-name: hazelcast.platform.demos.banking.transactionmonitor.AlertingToPostgresStore
     *       properties:
     *         address: '12.34.56.78'
     *         user: 'admin'
     * </pre>
     * <p>
     *
     * @param hazelcastInstance
     * @param existingIMapNames - maps that this run of the initialiser didn't create
     * @param postgresProperties - external db to connect to
     * @return true, always, either added or not needed
     */
    private static boolean dynamicMapConfig(HazelcastInstance hazelcastInstance,
            Set<String> existingIMapNames, Properties properties, String ourProjectProvenance,
            boolean localhost, boolean useViridian, TransactionMonitorFlavor transactionMonitorFlavor) {
        final String alertsWildcard = "alerts*";

        EventJournalConfig eventJournalConfig = new EventJournalConfig().setEnabled(true);
        boolean ok = true;

        //TODO && !useViridian
        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_ALERTS_LOG) && !useViridian) {
            MapConfig alertsMapConfig = new MapConfig(alertsWildcard);
            alertsMapConfig.setEventJournalConfig(eventJournalConfig);

            AlertingToPostgresMapStore alertingToPostgresMapStore = new AlertingToPostgresMapStore();

            MapStoreConfig mapStoreConfig = new MapStoreConfig().setEnabled(true);
            mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
            mapStoreConfig.setImplementation(alertingToPostgresMapStore);
            Properties postgresProperties = null;
            try {
                postgresProperties = MyUtils.getPostgresProperties(properties);
            } catch (Exception e) {
                LOGGER.error("dynamicMapConfig()", e);
                return false;
            }
            postgresProperties.put(MyConstants.PROJECT_PROVENANCE, ourProjectProvenance);
            mapStoreConfig.setProperties(postgresProperties);

            if (localhost) {
                LOGGER.info("localhost=={}, no map store for Postgres", localhost);
            } else {
                alertsMapConfig.setMapStoreConfig(mapStoreConfig);
                LOGGER.info("Postgres configured using: {}", alertsMapConfig.getMapStoreConfig().getProperties());
            }

            hazelcastInstance.getConfig().addMapConfig(alertsMapConfig);
        } else {
            LOGGER.info("Don't add journal to '{}', map already exists", MyConstants.IMAP_NAME_ALERTS_LOG);
            if (useViridian) {
                deleteForRetry(hazelcastInstance, useViridian, MyConstants.IMAP_NAME_ALERTS_LOG);
                ok = false;
            }
        }

        dynamicMapConfigJournalOnly(hazelcastInstance, existingIMapNames, useViridian);

        // Generic config, MapStore implementation is derived
        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_MYSQL_SLF4J)) {
            TransactionMonitorIdempotentInitializationMySql.defineMySql(hazelcastInstance, properties, transactionMonitorFlavor);

            MapConfig mySqlMapConfig = new MapConfig(MyConstants.IMAP_NAME_MYSQL_SLF4J);

            Properties mySqlProperties = TransactionMonitorIdempotentInitializationMySql.getMySqlProperties();

            MapStoreConfig mySqlStoreConfig = new MapStoreConfig().setEnabled(true)
            .setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER)
            .setClassName(GenericMapStore.class.getName()).setProperties(mySqlProperties);

            if (localhost) {
                LOGGER.info("localhost=={}, no map store for MySql", localhost);
            } else {
                mySqlMapConfig.setMapStoreConfig(mySqlStoreConfig);
                LOGGER.info("MySql configured using: {}", mySqlMapConfig.getMapStoreConfig().getProperties());
            }

            hazelcastInstance.getConfig().addMapConfig(mySqlMapConfig);
        } else {
            LOGGER.info("Don't add generic mapstore to '{}', map already exists", MyConstants.IMAP_NAME_MYSQL_SLF4J);
            if (useViridian) {
                deleteForRetry(hazelcastInstance, useViridian, MyConstants.IMAP_NAME_MYSQL_SLF4J);
                ok = false;
            }
        }

        return ok;
    }

    /**
     * <p>Delete so can recreate.
     * </p>
     *
     * @param hazelcastInstance
     * @param useViridian
     * @param mapName
     */
    private static void deleteForRetry(HazelcastInstance hazelcastInstance, boolean useViridian, String mapName) {
        LOGGER.info("'useViridian'=={}, destroying map '{}', bounce and try again", useViridian, mapName);
        hazelcastInstance.getMap(mapName).destroy();
    }

    /**
     * <p>Some maps only need event journals.
     * </p>
     * @param hazelcastInstance
     * @param existingIMapNames
     * @param useViridian
     */
    protected static void dynamicMapConfigJournalOnly(HazelcastInstance hazelcastInstance, Set<String> existingIMapNames,
            boolean useViridian) {
        for (String mapName : List.of(MyConstants.IMAP_NAME_HEAP, MyConstants.IMAP_NAME_MONGO_ACTIONS)) {
            if (!existingIMapNames.contains(mapName)) {
                EventJournalConfig eventJournalConfig = new EventJournalConfig().setEnabled(true);

                hazelcastInstance.getConfig()
                .addMapConfig(new MapConfig(mapName).setEventJournalConfig(eventJournalConfig));
            } else {
                LOGGER.info("Don't add journal to '{}', map already exists", mapName);
                if (useViridian) {
                    deleteForRetry(hazelcastInstance, useViridian, mapName);
                }
            }
        }
    }

    /**
     * <p>Maps that have indexes, currently just the transactions are made for
     * faster searching. When created manually it would be:
     * <pre>
     *     'transactions':
     *       indexes:
     *         - type: HASH
     *           attributes:
     *             - 'symbol'
     * </pre>
     * </p>
     * <p><b>addIndex()</b> replaces the definition, so would be idempotent.
     * However as it has a performance cost we skip if we know the map already
     * existed and so can presume it had the index
     * </p>
     *
     * @param hazelcastInstance
     * @param existingIMapNames - maps that this run of the initializer didn't create
     * @return true - Always.
     */
    private static boolean defineIndexes(HazelcastInstance hazelcastInstance, Set<String> existingIMapNames,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean useViridian) {

        // Only add if map hadn't previously existed and so has just been created
        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_TRANSACTIONS)) {
            IMap<?, ?> transactionsMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_TRANSACTIONS);

            String indexColumn1;
            switch (transactionMonitorFlavor) {
            case ECOMMERCE:
                indexColumn1 = "itemCode";
                break;
            case PAYMENTS:
                indexColumn1 = "bicCreditor";
                break;
            case TRADE:
            default:
                indexColumn1 = "symbol";
                break;
            }

            IndexConfig indexConfig = new IndexConfig();
            indexConfig.setName(MyConstants.IMAP_NAME_TRANSACTIONS + "_idx");
            indexConfig.setType(IndexType.HASH);
            indexConfig.setAttributes(Arrays.asList(indexColumn1));

            // Void method, hence returning true
            transactionsMap.addIndex(indexConfig);
        } else {
            LOGGER.trace("Don't add index to '{}', map already exists", MyConstants.IMAP_NAME_TRANSACTIONS);
            if (useViridian) {
                deleteForRetry(hazelcastInstance, useViridian, MyConstants.IMAP_NAME_TRANSACTIONS);
            }
        }

        return true;
    }

    /**
     * <p>Kafka properties can be stashed for ad-hoc jobs to use.
     * </p>
     * <p>Stock symbols are needed for transaction look-up enrichment,
     * the first member to start loads them from a file into
     * a {@link com.hazelcast.map.IMap}.
     * </p>
     */
    public static boolean loadNeededData(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarSource, boolean usePulsar, boolean useViridian, TransactionMonitorFlavor transactionMonitorFlavor) {
        boolean ok = true;
        try {
            IMap<String, String> jobConfigMap =
                    hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONFIG);

            if (!jobConfigMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", jobConfigMap.getName());
            } else {
                Properties properties = InitializerConfig.kafkaSourceProperties(bootstrapServers);
                jobConfigMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
                jobConfigMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        properties.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
                jobConfigMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        properties.getProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
                jobConfigMap.put(MyConstants.CASSANDRA_CONFIG_KEY,
                        System.getProperty(MyConstants.CASSANDRA_CONFIG_KEY, ""));
                jobConfigMap.put(MyConstants.MARIA_CONFIG_KEY, System.getProperty(MyConstants.MARIA_CONFIG_KEY, ""));
                jobConfigMap.put(MyConstants.MONGO_CONFIG_KEY, System.getProperty(MyConstants.MONGO_CONFIG_KEY, ""));
                jobConfigMap.put(MyConstants.MYSQL_CONFIG_KEY, System.getProperty(MyConstants.MYSQL_CONFIG_KEY, ""));
                jobConfigMap.put(MyConstants.POSTGRES_CONFIG_KEY,
                        System.getProperty(MyConstants.POSTGRES_CONFIG_KEY, ""));
                jobConfigMap.put(MyConstants.PULSAR_CONFIG_KEY, pulsarSource);
                jobConfigMap.put(MyConstants.PULSAR_OR_KAFKA_KEY, (usePulsar ? "pulsar" : "kafka"));
                jobConfigMap.put(MyConstants.TRANSACTION_MONITOR_FLAVOR, transactionMonitorFlavor.toString());
                jobConfigMap.put(MyConstants.USE_VIRIDIAN, Boolean.valueOf(useViridian).toString());

                LOGGER.trace("Loaded {} into '{}'", jobConfigMap.size(), jobConfigMap.getName());
            }

            loadNeededDataForFlavor(hazelcastInstance, transactionMonitorFlavor);
        } catch (Exception e) {
            LOGGER.error("loadNeededData()", e);
            ok = false;
        }
        return ok;
    }

    /**
     * <p>Load reference data appropriate to the flavor
     * </p>
     *
     * @param hazelcastInstance
     * @param transactionMonitorFlavor
     * @throws Exception
     */
    private static void loadNeededDataForFlavor(HazelcastInstance hazelcastInstance,
            TransactionMonitorFlavor transactionMonitorFlavor) throws Exception {
        IMap<String, BicInfo> bicsMap = null;
        IMap<String, ProductInfo> productsMap = null;
        IMap<String, SymbolInfo> symbolsMap = null;

        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            productsMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_PRODUCTS);
            if (!productsMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", productsMap.getName());
            } else {
                Map<String, ProductInfo> localMap = getProductInfoLocalMap();
                productsMap.putAll(localMap);

                LOGGER.trace("Loaded {} into '{}'", localMap.size(), productsMap.getName());
            }
            break;
        case PAYMENTS:
            bicsMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_BICS);
            if (!bicsMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", bicsMap.getName());
            } else {
                Map<String, BicInfo> localMap = getBicInfoLocalMap();
                bicsMap.putAll(localMap);

                LOGGER.trace("Loaded {} into '{}'", localMap.size(), bicsMap.getName());
            }
            break;
        case TRADE:
        default:
            symbolsMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);
            if (!symbolsMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", symbolsMap.getName());
            } else {
                Map<String, SymbolInfo> localMap = getSymbolInfoLocalMap();
                symbolsMap.putAll(localMap);

                LOGGER.trace("Loaded {} into '{}'", localMap.size(), symbolsMap.getName());
            }
            break;
        }
    }

    /**
     * <p>Format file "{@code productcatalog.txt}" for bulk insert.
     * </p>
     * @return
     * @throws Exception
     */
    private static Map<String, ProductInfo> getProductInfoLocalMap() throws Exception {
        return MyUtils.productCatalog().entrySet().stream()
                .collect(Collectors.<Entry<String, Tuple3<String, String, Double>>,
                        String, ProductInfo>
                        toUnmodifiableMap(
                        entry -> entry.getKey(),
                        entry -> {
                            ProductInfo productInfo = new ProductInfo();
                            productInfo.setItemName(entry.getValue().f0());
                            productInfo.setCategory(entry.getValue().f1());
                            productInfo.setPrice(entry.getValue().f2());
                            return productInfo;
                        }));
    }

    /**
     * <p>Format file "{@code biclist.txt}" for bulk insert.
     * </p>
     * @return
     * @throws Exception
     */
    private static Map<String, BicInfo> getBicInfoLocalMap() throws Exception {
        return MyUtils.bicList().entrySet().stream()
                .collect(Collectors.<Entry<String, Tuple4<String, Double, String, String>>,
                        String, BicInfo>
                        toUnmodifiableMap(
                        entry -> entry.getKey(),
                        entry -> {
                            String key = entry.getKey();
                            BicInfo bicInfo = new BicInfo();
                            bicInfo.setBankCode(key.substring(0, POS4));
                            bicInfo.setCountry(key.substring(POS4, POS6));
                            bicInfo.setCurrency(entry.getValue().f0());
                            bicInfo.setName(entry.getValue().f2());
                            bicInfo.setLocation(entry.getValue().f3());
                            bicInfo.setLocationCode(key.substring(POS6));
                            bicInfo.setExchangeRate(entry.getValue().f1());
                            return bicInfo;
                        }));
    }

    /**
     * <p>Format file "{@code nasdaqlisted.txt}" for bulk insert.
     * </p>
     * @return
     * @throws Exception
     */
    private static Map<String, SymbolInfo> getSymbolInfoLocalMap() throws Exception {
        return MyUtils.nasdaqListed().entrySet().stream()
                .collect(Collectors.<Entry<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>,
                        String, SymbolInfo>
                        toUnmodifiableMap(
                        entry -> entry.getKey(),
                        entry -> {
                            SymbolInfo symbolInfo = new SymbolInfo();
                            symbolInfo.setSecurityName(entry.getValue().f0());
                            symbolInfo.setMarketCategory(entry.getValue().f1());
                            symbolInfo.setFinancialStatus(entry.getValue().f2());
                            return symbolInfo;
                        }));
    }

    /**
     * <p>Define Hazelcast maps &amp; Kafka topics for later SQL querying.
     * </p>
     */
    public static boolean defineQueryableObjects(HazelcastInstance hazelcastInstance, String bootstrapServers,
            Properties properties, TransactionMonitorFlavor transactionMonitorFlavor,
            boolean isLocalhost, boolean isKubernetes, boolean useViridian) {
        boolean ok = true;
        ok &= defineKafka1(hazelcastInstance, bootstrapServers, transactionMonitorFlavor);
        ok &= defineKafka2(hazelcastInstance, bootstrapServers);
        if (!isLocalhost) {
            ok &= TransactionMonitorIdempotentInitializationCassandra.defineCassandra(hazelcastInstance,
                    properties, transactionMonitorFlavor);
            ok &= TransactionMonitorIdempotentInitializationMaria.defineMaria(hazelcastInstance,
                    properties, transactionMonitorFlavor, isKubernetes);
            ok &= TransactionMonitorIdempotentInitializationMongo.defineMongo(hazelcastInstance,
                    properties, transactionMonitorFlavor);
        }
        ok &= TransactionMonitorIdempotentInitializationAdmin.defineAdminIMaps(hazelcastInstance, useViridian);
        ok &= defineWANIMaps(hazelcastInstance, transactionMonitorFlavor);
        ok &= defineIMaps1(hazelcastInstance, transactionMonitorFlavor);
        ok &= defineIMaps2(hazelcastInstance, transactionMonitorFlavor);
        ok &= defineIMaps3(hazelcastInstance, transactionMonitorFlavor);
        ok &= defineIMaps4(hazelcastInstance, transactionMonitorFlavor);
        return ok;
    }


    /**
     * <p>Define Kafka streams so can be directly used as a
     * querying source by SQL.
     * </p>
     *
     * @param bootstrapServers
     */
    @SuppressWarnings("checkstyle:MethodLength")
    static boolean defineKafka1(HazelcastInstance hazelcastInstance, String bootstrapServers,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        String definition1a = "CREATE EXTERNAL MAPPING IF NOT EXISTS "
                // Name for our SQL
                + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS
                // Name of the remote object
                + " EXTERNAL NAME " + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS
                + " ( "
                + " id             VARCHAR, "
                + " price          DECIMAL, "
                + " quantity       BIGINT, "
                + " itemCode       VARCHAR, "
                // Timestamp is a reserved word, need to escape. Adjust the mapping name so avoiding clash with IMap
                + " \"timestamp\"  BIGINT "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java', 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        String definition1b = "CREATE EXTERNAL MAPPING IF NOT EXISTS "
                // Name for our SQL
                + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS
                // Name of the remote object
                + " EXTERNAL NAME " + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS
                + " ( "
                + " id             VARCHAR, "
                // Timestamp is a reserved word, need to escape. Adjust the mapping name so avoiding clash with IMap
                + " \"timestamp\"  BIGINT, "
                + " kind           VARCHAR, "
                + " bicCreditor    VARCHAR, "
                + " bicDebtor      VARCHAR, "
                + " ccy            VARCHAR, "
                + " amtFloor       DECIMAL, "
                + " xml            VARCHAR "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java', 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        String definition1c = "CREATE EXTERNAL MAPPING IF NOT EXISTS "
                // Name for our SQL
                + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS
                // Name of the remote object
                + " EXTERNAL NAME " + MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS
                + " ( "
                + " id             VARCHAR, "
                + " price          BIGINT, "
                + " quantity       BIGINT, "
                + " symbol         VARCHAR, "
                // Timestamp is a reserved word, need to escape. Adjust the mapping name so avoiding clash with IMap
                + " \"timestamp\"  BIGINT "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java', 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        boolean ok = true;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            ok = define(definition1a, hazelcastInstance);
            break;
        case PAYMENTS:
            ok = define(definition1b, hazelcastInstance);
            break;
        case TRADE:
        default:
            ok = define(definition1c, hazelcastInstance);
            break;
        }
        return ok;
    }
    /**
     * <p>Define more Kafka streams so can be directly used as a
     * querying source by SQL.
     * </p>
     *
     * @param bootstrapServers
     */
    static boolean defineKafka2(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        String definition2 = "CREATE EXTERNAL MAPPING IF NOT EXISTS "
                // Name for our SQL
                + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_ALERTS
                // Name of the remote object
                + " EXTERNAL NAME " + MyConstants.KAFKA_TOPIC_NAME_ALERTS
                + " ( "
                + "    __key BIGINT,"
                + "    this VARCHAR"
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.Long',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        boolean ok = true;
        ok = ok & define(definition2, hazelcastInstance);
        return ok;
    }

    /**
     * <p>Mappings only for WAN replicated IMap.
     * <p>
     *
     * @param hazelcastInstance
     * @return
     */
    static boolean defineWANIMaps(HazelcastInstance hazelcastInstance, TransactionMonitorFlavor transactionMonitorFlavor) {
        String definition3 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_AUDIT_LOG
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.Long',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
                + " )";

        String definition4 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_JOB_CONFIG
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + String.class.getName() + "'"
                + " )";

        String[] definition5Arr = getDefinition5();

        List<String> definitions;
        List<String> mapNames;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            definitions = List.of(definition3, definition4,
                    definition5Arr[TransactionMonitorFlavor.ECOMMERCE.ordinal()]);
            mapNames = MyConstants.WAN_IMAP_NAMES_ECOMMERCE;
            break;
        case PAYMENTS:
            definitions = List.of(definition3, definition4,
                    definition5Arr[TransactionMonitorFlavor.PAYMENTS.ordinal()]);
            mapNames = MyConstants.WAN_IMAP_NAMES_PAYMENTS;
            break;
        case TRADE:
        default:
            definitions = List.of(definition3, definition4,
                    definition5Arr[TransactionMonitorFlavor.TRADE.ordinal()]);
            mapNames = MyConstants.WAN_IMAP_NAMES_TRADE;
            break;
        }
        boolean ok = runDefine(definitions, hazelcastInstance);
        mapNames.forEach(mapName -> hazelcastInstance.getMap(mapName));
        if (definitions.size() != mapNames.size()) {
            LOGGER.error("Not all WAN maps defined");
            return false;
        }
        return ok;
    }

    /**
     * <p>The various styles of definition 5, depending on the required flavor.
     * </p>
     * @return
     */
    private static String[] getDefinition5() {
        String[] result = new String[TransactionMonitorFlavor.values().length];

        result[TransactionMonitorFlavor.ECOMMERCE.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_PRODUCTS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + ProductInfo.class.getName() + "'"
                + " )";

        result[TransactionMonitorFlavor.PAYMENTS.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_BICS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + BicInfo.class.getName() + "'"
                + " )";

        result[TransactionMonitorFlavor.TRADE.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_SYMBOLS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + SymbolInfo.class.getName() + "'"
                + " )";

        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                LOGGER.error("Definition 5 is missing for ordinal {}", i);
            }
        }
        return result;
    }

    /**
     * <p>Apply some definitions.
     * </p>
     *
     * @param definitions
     * @param hazelcastInstance
     * @return
     */
    protected static boolean runDefine(List<String> definitions, HazelcastInstance hazelcastInstance) {
        boolean ok = true;
        for (String definition : definitions) {
            ok &= define(definition, hazelcastInstance);
        }
        return ok;
    }

    /**
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     *
     * @param hazelcastInstance
     */
    static boolean defineIMaps1(HazelcastInstance hazelcastInstance, TransactionMonitorFlavor transactionMonitorFlavor) {
        String definition6 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple3.class.getName() + "'"
                + " )";

        // See also AggregateQuery writing to map, and Postgres table definition for MapStore
        String definition7 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_ALERTS_LOG
                + " ("
                + "    __key BIGINT,"
                + "    code VARCHAR,"
                + "    provenance VARCHAR,"
                + "    whence VARCHAR,"
                + "    volume BIGINT"
                + ")"
                 + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.Long',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getName() + "'"
                + " )";

        boolean ok = define(definition6, hazelcastInstance);
        ok &= define(definition7, hazelcastInstance);
        return ok;
    }

    /**
     * <p>More map definitions
     * </p>
     * @param hazelcastInstance
     */
    static boolean defineIMaps2(HazelcastInstance hazelcastInstance, TransactionMonitorFlavor transactionMonitorFlavor) {
        String[] definition8Arr = getDefinition8();

        boolean ok = true;
        List<String> definitions
        = List.of(definition8Arr[transactionMonitorFlavor.ordinal()]);

        for (String definition : definitions) {
            ok &= define(definition, hazelcastInstance);
        }
        return ok;
    }

    /**
     * <p>The various styles of definition 8, depending on the required flavor.
     * </p>
     * @return
     */
    @SuppressWarnings("checkstyle:MethodLength")
    private static String[] getDefinition8() {
        String[] result = new String[TransactionMonitorFlavor.values().length];

        result[TransactionMonitorFlavor.ECOMMERCE.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                        + MyConstants.IMAP_NAME_PERSPECTIVE
                        + " ("
                        + "    __key VARCHAR,"
                        + "    code VARCHAR,"
                        + "    \"count\" BIGINT,"
                        + "    \"sum\" DOUBLE,"
                        + "    average DOUBLE,"
                        + "    seconds INTEGER,"
                        + "    random INTEGER"
                        + ")"
                        + " TYPE IMap "
                        + " OPTIONS ( "
                        + " 'keyFormat' = 'java',"
                        + " 'keyJavaClass' = '" + String.class.getName() + "',"
                        + " 'valueFormat' = 'compact',"
                        + " 'valueCompactTypeName' = '" + PerspectiveEcommerce.class.getSimpleName() + "'"
                        + " )";

        result[TransactionMonitorFlavor.PAYMENTS.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                        + MyConstants.IMAP_NAME_PERSPECTIVE
                        + " ("
                        + "    __key VARCHAR,"
                        + "    bic VARCHAR,"
                        + "    \"count\" BIGINT,"
                        + "    \"sum\" DOUBLE,"
                        + "    average DOUBLE,"
                        + "    seconds INTEGER,"
                        + "    random INTEGER"
                        + ")"
                        + " TYPE IMap "
                        + " OPTIONS ( "
                        + " 'keyFormat' = 'java',"
                        + " 'keyJavaClass' = '" + String.class.getName() + "',"
                        + " 'valueFormat' = 'compact',"
                        + " 'valueCompactTypeName' = '" + PerspectivePayments.class.getSimpleName() + "'"
                        + " )";

        result[TransactionMonitorFlavor.TRADE.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                        + MyConstants.IMAP_NAME_PERSPECTIVE
                        + " ("
                        + "    __key VARCHAR,"
                        + "    symbol VARCHAR,"
                        + "    \"count\" BIGINT,"
                        + "    \"sum\" DOUBLE,"
                        + "    latest DOUBLE,"
                        + "    seconds INTEGER,"
                        + "    random INTEGER"
                        + ")"
                        + " TYPE IMap "
                        + " OPTIONS ( "
                        + " 'keyFormat' = 'java',"
                        + " 'keyJavaClass' = '" + String.class.getName() + "',"
                        + " 'valueFormat' = 'compact',"
                        + " 'valueCompactTypeName' = '" + PerspectiveTrade.class.getSimpleName() + "'"
                        + " )";

        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                LOGGER.error("Definition 8 is missing for ordinal {}", i);
            }
        }
        return result;
    }

    /**
     * <p>Even more map definitions
     * </p>
     * @param hazelcastInstance
     */
    static boolean defineIMaps3(HazelcastInstance hazelcastInstance, TransactionMonitorFlavor transactionMonitorFlavor) {
       String[] definition9Arr = getDefinition9();
       String definition9 = definition9Arr[transactionMonitorFlavor.ordinal()];

       String[] definition9ExtraArr = getDefinition9Extra();
       String definition9Extra = definition9ExtraArr[transactionMonitorFlavor.ordinal()];

       String definition10 = "CREATE MAPPING IF NOT EXISTS "
               + MyConstants.IMAP_NAME_PYTHON_SENTIMENT
               + " TYPE IMap "
               + " OPTIONS ( "
               + " 'keyFormat' = 'java',"
               + " 'keyJavaClass' = 'java.lang.String',"
               + " 'valueFormat' = 'java',"
               + " 'valueJavaClass' = 'java.lang.String'"
               + " )";

       boolean ok = true;
       List<String> definitions;
       if (definition9Extra == null || definition9Extra.isBlank()) {
           definitions = List.of(definition9, definition10);
       } else {
           definitions = List.of(definition9, definition9Extra, definition10);
       }

       for (String definition : definitions) {
           ok &= define(definition, hazelcastInstance);
       }
       return ok;
    }

    /**
     * <p>The various styles of definition 9, depending on the required flavor.
     * </p>
     * @return
     */
    @SuppressWarnings("checkstyle:MethodLength")
    private static String[] getDefinition9() {
        String[] result = new String[TransactionMonitorFlavor.values().length];

        result[TransactionMonitorFlavor.ECOMMERCE.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRANSACTIONS
                + " ("
                + "    __key VARCHAR,"
                + "    id VARCHAR,"
                + "    \"timestamp\" BIGINT,"
                + "    itemCode VARCHAR,"
                + "    price DECIMAL,"
                + "    quantity BIGINT"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getName() + "'"
                + " )";

        result[TransactionMonitorFlavor.PAYMENTS.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRANSACTIONS
                + " ("
                + "    __key VARCHAR,"
                + "    id VARCHAR,"
                + "    \"timestamp\" BIGINT,"
                + "    kind VARCHAR,"
                + "    bicCreditor VARCHAR,"
                + "    bicDebitor VARCHAR,"
                + "    ccy VARCHAR,"
                + "    amtFloor DECIMAL"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getName() + "'"
                + " )";

        result[TransactionMonitorFlavor.TRADE.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRANSACTIONS
                + " ("
                + "    __key VARCHAR,"
                + "    id VARCHAR,"
                + "    \"timestamp\" BIGINT,"
                + "    symbol VARCHAR,"
                + "    price DECIMAL,"
                + "    quantity BIGINT"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getName() + "'"
                + " )";

        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                LOGGER.error("Definition 9 is missing for ordinal {}", i);
            }
        }
        return result;
    }

    /**
     * <p>Bonus definitions for transactions, may not be needed depending
     * on the type of transaction.
     * </p>
     * @return
     */
    private static String[] getDefinition9Extra() {
        String[] result = new String[TransactionMonitorFlavor.values().length];

        result[TransactionMonitorFlavor.ECOMMERCE.ordinal()] = "";

        result[TransactionMonitorFlavor.PAYMENTS.ordinal()] =
                "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRANSACTIONS_XML
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'varchar',"
                + " 'valueFormat' = 'varchar'"
                + " )";

        result[TransactionMonitorFlavor.TRADE.ordinal()] = "";

        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                LOGGER.error("Definition 9 is missing for ordinal {}", i);
            }
        }
        return result;
    }

    /**
     * <p>Even more map definitions
     * </p>
     * @param hazelcastInstance
     */
    static boolean defineIMaps4(HazelcastInstance hazelcastInstance, TransactionMonitorFlavor transactionMonitorFlavor) {
        String definition11 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_JOB_CONTROL
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'varchar',"
                + " 'valueFormat' = 'varchar'"
                + " )";

        // Not much of view, but shows the concept
        String definition12 =  "CREATE OR REPLACE VIEW "
                + MyConstants.IMAP_NAME_TRANSACTIONS + MyConstants.VIEW_SUFFIX
                + " AS SELECT "
                + "    __key"
                + "      AS \"primary_key\""
                + " FROM " + MyConstants.IMAP_NAME_TRANSACTIONS;

        boolean ok = true;
        List<String> definitions;

        // Same for all currently
        switch (transactionMonitorFlavor) {
        //case ECOMMERCE:
        //    definitions = List.of(definition11, definition12);
        //    break;
        //case PAYMENTS:
        //    definitions = List.of(definition11, definition12);
        //    break;
        //case TRADE:
        default:
            definitions = List.of(definition11, definition12);
            break;
        }
        for (String definition : definitions) {
            ok &= define(definition, hazelcastInstance);
        }
        return ok;
    }

    /**
     * <p>Generic handler to loading definitions
     * </p>
     *
     * @param definition
     * @param hazelcastInstance
     */
    static boolean define(String definition, HazelcastInstance hazelcastInstance) {
        LOGGER.debug("Definition '{}'", definition);
        if (definition == null || definition.isBlank()) {
            LOGGER.error("Empty definition");
            return false;
        }
        try {
            hazelcastInstance.getSql().execute(definition);
            return true;
        } catch (Exception e) {
            LOGGER.error(definition, e);
            return false;
        }
    }

    /**
     * <p><i>1</i> Launch a job to read transactions from Kafka and place them in a map,
     * a simple upload.
     * </p>
     * <p><i>2</i> Launch a job to read the same transactions from Kafka and to aggregate
     * them, placing the results into another map.
     * </p>
     * <p>As we launch them at the same time, the 2nd job could be merged into the
     * 1st, and use the same input. However, here we keep them separate for clarity.
     * </p>
     * <p>Both jobs need the Kafka connection, a list of brokers.
     * </p>
     * @param properties
     */
    public static boolean launchNeededJobs(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarAddress, Properties properties, String clusterName,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean kubernetes) {
        boolean ok = true;
        String projectName = properties.getOrDefault(UtilsConstants.SLACK_PROJECT_NAME,
                TransactionMonitorIdempotentInitialization.class.getSimpleName()).toString();

        String pulsarOrKafka = hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_JOB_CONFIG).get(MyConstants.PULSAR_OR_KAFKA_KEY).toString();
        boolean usePulsar = MyUtils.usePulsar(pulsarOrKafka);
        logUsePulsar(usePulsar, pulsarOrKafka);

        String kubernetesOrViridian = hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_JOB_CONFIG).get(MyConstants.USE_VIRIDIAN).toString();
        boolean useViridian = MyUtils.useViridian(kubernetesOrViridian);
        logUseViridian(useViridian, kubernetesOrViridian);

        // CP needs a big enough cluster, only known to be true for Kubernetes
        //TODO Not yet available on Viridian @ January 2024.
        boolean useCP = kubernetes & (!useViridian);

        if (System.getProperty("my.autostart.enabled", "").equalsIgnoreCase("false")) {
            LOGGER.info("Not launching transaction input jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));
        } else {
            LOGGER.info("Launching transaction input jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));
            try {
                if (!usePulsar) {
                    pulsarAddress = null;
                }
                ok = launchTransactionJobs(hazelcastInstance, bootstrapServers, pulsarAddress,
                        useViridian, projectName, clusterName, transactionMonitorFlavor, useCP);
                if (!ok) {
                    return ok;
                }
            } catch (Exception e) {
                LOGGER.error("launchNeededJobs(): launchTransactionJobs()", e);
                return false;
            }
        }

        if (ok) {
            ok &= doSql(usePulsar, hazelcastInstance, bootstrapServers, transactionMonitorFlavor);
        }

        // Remaining jobs need properties
        if (properties.size() == 0) {
            LOGGER.error("launchNeededJobs: properties is empty");
            return false;
        }

        if (ok) {
            // Slack SQL integration (reading/writing) from common utils
            ok &= launchSlackReadWrite(useViridian, projectName, hazelcastInstance, properties, transactionMonitorFlavor);
        }

        if (ok) {
            try {
                // Feed changes from Postgres directly into Hazelcast
                Properties postgresProperties = MyUtils.getPostgresProperties(properties);
                ok &= launchPostgresCDC(hazelcastInstance, postgresProperties,
                        Objects.toString(properties.get(MyConstants.PROJECT_PROVENANCE)), useViridian);
            } catch (Exception e) {
                LOGGER.error("launchNeededJobs: postgresProperties", e);
                ok = false;
            }
        }

        // Stop/start the archiver
        ok &= launchArchiverStateController(hazelcastInstance, ok);

        // Optional
        MyUtils.logStuff(hazelcastInstance);
        return ok;
    }

    /**
     * <p>SQL style jobs, no pipelines!
     * </p>
     *
     * @param usePulsar
     * @param hazelcastInstance
     * @param bootstrapServers
     * @param transactionMonitorFlavor
     * @return
     */
    private static boolean doSql(boolean usePulsar, HazelcastInstance hazelcastInstance, String bootstrapServers,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        boolean ok = true;

        if (!usePulsar) {
            // SQL jobs that do use Kafka, so when Pulsar isn't being used
            ok &=  TransactionMonitorIdempotentInitializationSql.launchKafkaSqlJobs(hazelcastInstance,
                    bootstrapServers, transactionMonitorFlavor);
        }
        if (ok) {
            // SQL jobs that don't use Kafka
            ok &=  TransactionMonitorIdempotentInitializationSql.launchNonKafkaSqlJobs(hazelcastInstance);
        }

        return ok;
    }

    /**
     * <p>The main two jobs upload and aggregate the transaction input feed.
     * This is done as two jobs for simplicity, but would be more efficient to have one reader on the
     * input feed sending data to two legs of processing.
     * </p>
     *
     * @param hazelcastInstance
     * @param bootstrapServers
     * @param pulsarAddresss
     * @param clusterName
     * @param useViridian
     * @param projectName
     * @param clusterName
     * @param transactionMonitorFlavor
     * @param useCP
     * @return
     */
    public static boolean launchTransactionJobs(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarAddress, boolean useViridian, String projectName, String clusterName,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean useCP) {

        /* Transaction ingest
         */
        JobConfig jobConfigIngestTransactions = new JobConfig();
        jobConfigIngestTransactions.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
        jobConfigIngestTransactions.setName(IngestTransactions.class.getSimpleName());
        jobConfigIngestTransactions.addClass(IngestTransactions.class);

        StreamStage<Entry<String, HazelcastJsonValue>> pulsarInputSource1 = null;
        if (pulsarAddress != null) {
            // Attach Pulsar classes only if needed
            pulsarInputSource1 = MyPulsarSource.inputSourceKeyAndJson(pulsarAddress);
            jobConfigIngestTransactions.addClass(MyPulsarSource.class);
        }

        Pipeline pipelineIngestTransactions = IngestTransactions.buildPipeline(bootstrapServers,
                pulsarInputSource1, transactionMonitorFlavor);

        if (pulsarAddress != null && useViridian) {
            //TODO Not yet available on Viridian @ January 2024.
            LOGGER_TO_IMAP.error("Pulsar is not currently supported on Viridian");
            return false;
        } else {
            Job job = UtilsJobs.myNewJobIfAbsent(LOGGER,
                    hazelcastInstance, pipelineIngestTransactions, jobConfigIngestTransactions);
            if (job != null) {
                LOGGER_TO_IMAP.info(Objects.toString(job));
            }
        }

        /* Transaction aggregation
         */
        JobConfig jobConfigAggregateQuery = new JobConfig();
        jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
        jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName());
        jobConfigAggregateQuery.addClass(AggregateQuery.class);
        jobConfigAggregateQuery.addClass(MaxAggregator.class);
        jobConfigAggregateQuery.addClass(UtilsFormatter.class);

        StreamStage<?> pulsarInputSource2 = null;
        if (pulsarAddress != null) {
            // Attach Pulsar classes only if needed
            pulsarInputSource2 = MyPulsarSource.inputSourceTransaction(pulsarAddress, transactionMonitorFlavor);
            jobConfigAggregateQuery.addClass(MyPulsarSource.class);
        }

        Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(bootstrapServers,
                pulsarInputSource2, projectName, jobConfigAggregateQuery.getName(),
                clusterName, transactionMonitorFlavor, useCP);

        if (pulsarAddress != null && useViridian) {
            //TODO Not yet available on Viridian @ January 2024.
            LOGGER_TO_IMAP.error("Pulsar is not currently supported on Viridian");
            return false;
        } else {
            Job job = UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAggregateQuery, jobConfigAggregateQuery);
            if (job != null) {
                LOGGER_TO_IMAP.info(Objects.toString(job));
            }
        }

        return true;
    }

    /**
     * <p>Helper for logging.
     * </p>
     * @param usePulsar
     * @param pulsarOrKafka
     */
    private static void logUsePulsar(boolean usePulsar, String pulsarOrKafka) {
        if (usePulsar) {
            LOGGER.info("Using Pulsar = '{}'=='{}'", MyConstants.PULSAR_OR_KAFKA_KEY, pulsarOrKafka);
        } else {
            LOGGER.info("Using Kafka = '{}'=='{}'", MyConstants.PULSAR_OR_KAFKA_KEY, pulsarOrKafka);
        }
    }

    /**
     * <p>Helper for logging.
     * </p>
     * @param useViridian
     * @param kubernetesOrViridian
     */
    private static void logUseViridian(boolean useViridian, String kubernetesOrViridian) {
        if (useViridian) {
            LOGGER.info("Using Viridian => '{}'=='{}'", MyConstants.USE_VIRIDIAN, kubernetesOrViridian);
        } else {
            LOGGER.info("Not using Viridian => '{}'=='{}'", MyConstants.USE_VIRIDIAN, kubernetesOrViridian);
        }
    }

    /**
     * <p>Launch Slack jobs for SQL (read/write) and alerting (write) if credentials provided.
     * </p>
     *
     * @param useViridian
     * @param projectName
     * @param hazelcastInstance
     * @param properties
     */
    private static boolean launchSlackReadWrite(boolean useViridian, Object projectName,
            HazelcastInstance hazelcastInstance, Properties properties,
            TransactionMonitorFlavor transactionMonitorFlavor) {

        String slackAccessToken = Objects.toString(properties.get(UtilsConstants.SLACK_ACCESS_TOKEN));
        String slackChannelId = Objects.toString(properties.get(UtilsConstants.SLACK_CHANNEL_ID));
        String slackChannelName = Objects.toString(properties.get(UtilsConstants.SLACK_CHANNEL_NAME));

        if (slackAccessToken.length() < UtilsSlack.REASONABLE_MINIMAL_LENGTH_FOR_SLACK_PROPERTY) {
            LOGGER.warn("No Slack jobs, '{}' too short: '{}'", UtilsConstants.SLACK_ACCESS_TOKEN, slackAccessToken);
            return true;
        }
        if (slackChannelId.length() < UtilsSlack.REASONABLE_MINIMAL_LENGTH_FOR_SLACK_PROPERTY) {
            LOGGER.warn("No Slack jobs, '{}' too short: '{}'", UtilsConstants.SLACK_CHANNEL_ID, slackChannelId);
            return true;
        }
        if (slackChannelName.length() < UtilsSlack.REASONABLE_MINIMAL_LENGTH_FOR_SLACK_PROPERTY) {
            LOGGER.warn("No Slack jobs, '{}' too short: '{}'", UtilsConstants.SLACK_CHANNEL_NAME, slackChannelName);
            return true;
        }

        // Slack alerting (writing), indirectly uses common utils
        if (useViridian) {
            //TODO Not yet available on Viridian @ January 2024.
            LOGGER.error("Slack is not currently supported on Viridian");
        } else {
            try {
                UtilsSlackSQLJob.submitJob(hazelcastInstance, projectName == null ? "" : projectName.toString());
            } catch (Exception e) {
                LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
                return false;
            }
            launchSlackJob(hazelcastInstance, properties, transactionMonitorFlavor);
        }

        return true;
    }

    /**
     * <p>Optional, but really cool, job for integration with Slack.
     * </p>
     * @param hazelcastInstance
     * @param properties
     */
    private static boolean launchSlackJob(HazelcastInstance hazelcastInstance, Properties properties,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        try {
            Pipeline pipelineAlertingToSlack = AlertingToSlack.buildPipeline(
                    properties.get(UtilsConstants.SLACK_ACCESS_TOKEN),
                    properties.get(UtilsConstants.SLACK_CHANNEL_NAME),
                    properties.get(UtilsConstants.SLACK_PROJECT_NAME),
                    properties.get(UtilsConstants.SLACK_BUILD_USER),
                    transactionMonitorFlavor
                    );

            JobConfig jobConfigAlertingToSlack = new JobConfig();
            jobConfigAlertingToSlack.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigAlertingToSlack.setName(AlertingToSlack.class.getSimpleName());
            jobConfigAlertingToSlack.addClass(AlertingToSlack.class);
            jobConfigAlertingToSlack.addClass(UtilsSlackSink.class);

            Job job = UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAlertingToSlack, jobConfigAlertingToSlack);
            if (job != null) {
                LOGGER_TO_IMAP.info(Objects.toString(job));
            }
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + AlertingToSlack.class.getSimpleName(), e);
            return false;
        }
        return true;
    }

    /**
     * <p>Launch a job to read changes from Postgres (that you can make
     * by directly connecting) into Hazelcast
     * </p>
     *
     * @param hazelcastInstance
     * @param properties
     */
    private static boolean launchPostgresCDC(HazelcastInstance hazelcastInstance,
            Properties properties, String ourProjectProvenance, boolean useViridian) {

        try {
            Pipeline pipelinePostgresCDC = PostgresCDC.buildPipeline(
                    Objects.toString(properties.get(MyConstants.POSTGRES_ADDRESS)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_DATABASE)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_SCHEMA)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_USER)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_PASSWORD)),
                    hazelcastInstance.getConfig().getClusterName(),
                    MyConstants.IMAP_NAME_ALERTS_LOG,
                    ourProjectProvenance,
                    useViridian
                    );

            JobConfig jobConfigPostgresCDC = new JobConfig();
            jobConfigPostgresCDC.setName(PostgresCDC.class.getSimpleName());
            jobConfigPostgresCDC.addClass(PostgresCDC.class);

            Job job = UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelinePostgresCDC, jobConfigPostgresCDC);
            if (job != null) {
                LOGGER_TO_IMAP.info(Objects.toString(job));
            }
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + PostgresCDC.class.getSimpleName(), e);
            return false;
        }
        return true;
    }

    /**
     * <p>Listens on {@link MyConstants.IMAP_NAME_MONGO_ACTIONS} for actions
     * fed from Mongo CDC
     * </p>
     *
     * @param hazelcastInstance
     * @param Have previous jobs been successfully submitted
     * @return
     */
    private static boolean launchArchiverStateController(HazelcastInstance hazelcastInstance, boolean ok) {
        if (!ok) {
            // Already failed, do no more
            return false;
        }

        try {
            Pipeline pipelineArchiverStateController = AlertLoggerManager.buildPipeline();

            JobConfig jobConfigArchiverStateController = new JobConfig();
            jobConfigArchiverStateController.setName(AlertLoggerManager.class.getSimpleName());
            jobConfigArchiverStateController.addClass(AlertLoggerManager.class);

            Job job = UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineArchiverStateController,
                    jobConfigArchiverStateController);
            if (job != null) {
                LOGGER_TO_IMAP.info(Objects.toString(job));
            }
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + AlertLoggerManager.class.getSimpleName(), e);
            return false;
        }
        return true;
    }
}
