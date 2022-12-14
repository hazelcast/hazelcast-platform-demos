/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
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
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.JobConfigArguments;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.utils.UtilsConstants;
import com.hazelcast.platform.demos.utils.UtilsFormatter;
import com.hazelcast.platform.demos.utils.UtilsJobs;
import com.hazelcast.platform.demos.utils.UtilsSlackSQLJob;
import com.hazelcast.platform.demos.utils.UtilsSlackSink;

/**
 * <p>May be invoked from clientside or serverside to ensure serverside ready.
 * </p>
 * <p>Has to be idempotent, so a client can call at start-up without
 * having to test if another client has already run it.
 * </p>
 */
public class CommonIdempotentInitialization {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonIdempotentInitialization.class);

    /**
     * <p>Maps and mappings needed for WAN, rather than replicate "{@code __sql.catalog}"
     * </p>
     */
    public static boolean createMinimal(HazelcastInstance hazelcastInstance,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        return defineWANIMaps(hazelcastInstance, transactionMonitorFlavor);
    }

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
            Properties postgresProperties, String ourProjectProvenance,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean localhost) {
        // Capture what was present before
        Set<String> existingIMapNames = hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> distributedObject instanceof IMap)
                .map(distributedObject -> distributedObject.getName())
                .filter(name -> !name.startsWith("__"))
                .collect(Collectors.toCollection(TreeSet::new));

        // Add journals and map stores to maps before they are created
        boolean ok = dynamicMapConfig(hazelcastInstance, existingIMapNames,
                postgresProperties, ourProjectProvenance, localhost);

        // Accessing non-existing maps does not return any failures
        List<String> iMapNames;
        switch (transactionMonitorFlavor) {
            case ECOMMERCE:
                iMapNames = MyConstants.IMAP_NAMES_ECOMMERCE;
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
            ok = defineIndexes(hazelcastInstance, existingIMapNames, transactionMonitorFlavor);
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
            Set<String> existingIMapNames, Properties postgresProperties, String ourProjectProvenance,
            boolean localhost) {
        final String alertsWildcard = "alerts*";

        EventJournalConfig eventJournalConfig = new EventJournalConfig();
        eventJournalConfig.setEnabled(true);

        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_ALERTS_LOG)) {
            MapConfig alertsMapConfig = new MapConfig(alertsWildcard);
            alertsMapConfig.setEventJournalConfig(eventJournalConfig);

            AlertingToPostgresMapStore alertingToPostgresMapStore
                = new AlertingToPostgresMapStore();

            MapStoreConfig mapStoreConfig = new MapStoreConfig();
            mapStoreConfig.setEnabled(true);
            mapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
            mapStoreConfig.setImplementation(alertingToPostgresMapStore);
            Properties properties = new Properties();
            properties.putAll(postgresProperties);
            properties.put(MyConstants.PROJECT_PROVENANCE, ourProjectProvenance);
            mapStoreConfig.setProperties(properties);

            if (localhost) {
                LOGGER.info("localhost=={}, no map store for Postgres", localhost);
            } else {
                alertsMapConfig.setMapStoreConfig(mapStoreConfig);
            }

            hazelcastInstance.getConfig().addMapConfig(alertsMapConfig);
        } else {
            LOGGER.trace("Don't add journal to '{}', map already exists", MyConstants.IMAP_NAME_ALERTS_LOG);
        }

        return true;
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
            TransactionMonitorFlavor transactionMonitorFlavor) {

        // Only add if map hadn't previously existed and so has just been created
        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_TRANSACTIONS)) {
            IMap<?, ?> transactionsMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_TRANSACTIONS);

            String indexColumn1;
            switch (transactionMonitorFlavor) {
            case ECOMMERCE:
                indexColumn1 = "itemCode";
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
            String pulsarList, boolean usePulsar, boolean useHzCloud, TransactionMonitorFlavor transactionMonitorFlavor) {
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
                jobConfigMap.put(MyConstants.PULSAR_CONFIG_KEY, pulsarList);
                if (usePulsar) {
                    jobConfigMap.put(MyConstants.PULSAR_OR_KAFKA_KEY, "pulsar");
                } else {
                    jobConfigMap.put(MyConstants.PULSAR_OR_KAFKA_KEY, "kafka");
                }
                jobConfigMap.put(MyConstants.USE_HZ_CLOUD, Boolean.valueOf(useHzCloud).toString());

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
        IMap<String, ProductInfo> productsMap = null;
        IMap<String, SymbolInfo> symbolsMap = null;

        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            productsMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_PRODUCTS);
            if (!productsMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", productsMap.getName());
            } else {
                Map<String, ProductInfo> localMap =
                        MyUtils.productCatalog().entrySet().stream()
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

                productsMap.putAll(localMap);

                LOGGER.trace("Loaded {} into '{}'", localMap.size(), productsMap.getName());
            }
            break;
        case TRADE:
        default:
            symbolsMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);
            if (!symbolsMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", symbolsMap.getName());
            } else {
                Map<String, SymbolInfo> localMap =
                        MyUtils.nasdaqListed().entrySet().stream()
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

                symbolsMap.putAll(localMap);

                LOGGER.trace("Loaded {} into '{}'", localMap.size(), symbolsMap.getName());
            }
            break;
        }
    }

    /**
     * <p>Define Hazelcast maps &amp; Kafka topics for later SQL querying.
     * </p>
     */
    public static boolean defineQueryableObjects(HazelcastInstance hazelcastInstance, String bootstrapServers,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        boolean ok = true;
        ok &= defineKafka1(hazelcastInstance, bootstrapServers, transactionMonitorFlavor);
        ok &= defineKafka2(hazelcastInstance, bootstrapServers);
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
                + " itemCode      VARCHAR, "
                // Timestamp is a reserved word, need to escape. Adjust the mapping name so avoiding clash with IMap
                + " \"timestamp\"  BIGINT "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
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
                + " price          BIGINT, "
                + " quantity       BIGINT, "
                + " symbol         VARCHAR, "
                // Timestamp is a reserved word, need to escape. Adjust the mapping name so avoiding clash with IMap
                + " \"timestamp\"  BIGINT "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        boolean ok = true;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            ok = define(definition1a, hazelcastInstance);
            break;
        case TRADE:
        default:
            ok = define(definition1b, hazelcastInstance);
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

        String definition5a = "CREATE MAPPING IF NOT EXISTS "
                 + MyConstants.IMAP_NAME_PRODUCTS
                 + " TYPE IMap "
                 + " OPTIONS ( "
                 + " 'keyFormat' = 'java',"
                 + " 'keyJavaClass' = 'java.lang.String',"
                 + " 'valueFormat' = 'java',"
                 + " 'valueJavaClass' = '" + ProductInfo.class.getName() + "'"
                 + " )";

        String definition5b = "CREATE MAPPING IF NOT EXISTS "
                 + MyConstants.IMAP_NAME_SYMBOLS
                 + " TYPE IMap "
                 + " OPTIONS ( "
                 + " 'keyFormat' = 'java',"
                 + " 'keyJavaClass' = 'java.lang.String',"
                 + " 'valueFormat' = 'java',"
                 + " 'valueJavaClass' = '" + SymbolInfo.class.getName() + "'"
                 + " )";

        List<String> definitions;
        List<String> mapNames;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            definitions = List.of(definition3, definition4, definition5a);
            mapNames = MyConstants.WAN_IMAP_NAMES_ECOMMERCE;
            break;
        case TRADE:
        default:
            definitions = List.of(definition3, definition4, definition5b);
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
     * <p>Apply some definitions.
     * </p>
     *
     * @param definitions
     * @param hazelcastInstance
     * @return
     */
    private static boolean runDefine(List<String> definitions, HazelcastInstance hazelcastInstance) {
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
        String definition8a = "CREATE MAPPING IF NOT EXISTS "
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
                + " 'valueCompactTypeName' = '" + PerspectiveTrade.class.getSimpleName() + "'"
                + " )";
        String definition8b = "CREATE MAPPING IF NOT EXISTS "
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

        boolean ok = true;
        List<String> definitions;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            definitions = List.of(definition8a);
            break;
        case TRADE:
        default:
            definitions = List.of(definition8b);
            break;
        }
        for (String definition : definitions) {
            ok &= define(definition, hazelcastInstance);
        }
        return ok;
    }

    /**
     * <p>Even more map definitions
     * </p>
     * @param hazelcastInstance
     */
    static boolean defineIMaps3(HazelcastInstance hazelcastInstance, TransactionMonitorFlavor transactionMonitorFlavor) {
        String definition9a = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRANSACTIONS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + TransactionEcommerce.class.getName() + "'"
                + " )";
       String definition9b = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRANSACTIONS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + TransactionTrade.class.getName() + "'"
                + " )";

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
       switch (transactionMonitorFlavor) {
       case ECOMMERCE:
           definitions = List.of(definition9a, definition10);
           break;
       case TRADE:
       default:
           definitions = List.of(definition9b, definition10);
           break;
       }
       for (String definition : definitions) {
           ok &= define(definition, hazelcastInstance);
       }
       return ok;
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
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
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

        // Currently same definition
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            definitions = List.of(definition11, definition12);
            break;
        case TRADE:
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
            String pulsarList, Properties postgresProperties, Properties properties, String clusterName,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        String projectName = properties.getOrDefault(UtilsConstants.SLACK_PROJECT_NAME,
                CommonIdempotentInitialization.class.getSimpleName()).toString();

        String pulsarOrKafka = hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_JOB_CONFIG).get(MyConstants.PULSAR_OR_KAFKA_KEY).toString();
        boolean usePulsar = MyUtils.usePulsar(pulsarOrKafka);
        logUsePulsar(usePulsar, pulsarOrKafka);

        String cloudOrHzCloud = hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_JOB_CONFIG).get(MyConstants.USE_HZ_CLOUD).toString();
        boolean useHzCloud = MyUtils.useHzCloud(cloudOrHzCloud);
        logUseHzCloud(useHzCloud, cloudOrHzCloud);

        if (System.getProperty("my.autostart.enabled", "").equalsIgnoreCase("false")) {
            LOGGER.info("Not launching Kafka jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));
        } else {
            LOGGER.info("Launching Kafka jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));

            // Transaction ingest
            Pipeline pipelineIngestTransactions = IngestTransactions.buildPipeline(bootstrapServers, pulsarList, usePulsar);

            JobConfig jobConfigIngestTransactions = new JobConfig();
            jobConfigIngestTransactions.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigIngestTransactions.setName(IngestTransactions.class.getSimpleName());
            jobConfigIngestTransactions.addClass(IngestTransactions.class);

            if (usePulsar && useHzCloud) {
                //TODO Fix once supported by HZ Cloud
                LOGGER.error("Pulsar is not currently supported on Hazelcast Cloud");
            } else {
                UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineIngestTransactions, jobConfigIngestTransactions);
            }

            // Transaction aggregation
            JobConfig jobConfigAggregateQuery = new JobConfig();
            jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName());
            jobConfigAggregateQuery.addClass(AggregateQuery.class);
            jobConfigAggregateQuery.addClass(MaxAggregator.class);
            jobConfigAggregateQuery.addClass(UtilsFormatter.class);

            Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(bootstrapServers,
                    pulsarList, usePulsar, projectName, jobConfigAggregateQuery.getName(),
                    clusterName, transactionMonitorFlavor);

            if (usePulsar && useHzCloud) {
                //TODO Fix once supported by HZ Cloud
                LOGGER.error("Pulsar is not currently supported on Hazelcast Cloud");
            } else {
                UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAggregateQuery, jobConfigAggregateQuery);
                // Aggregate query creates alerts to an IMap. Use a separate rather than same job to copy to Kafka.
                launchAlertsToKafkaAndToLog(hazelcastInstance, bootstrapServers, transactionMonitorFlavor);
            }

        }

        // Remaining jobs need properties
        if (properties.size() == 0) {
            LOGGER.error("launchNeededJobs: properties is empty");
            return false;
        }

        // Slack SQL integration (reading/writing) from common utils
        launchSlackReadWrite(useHzCloud, projectName, hazelcastInstance, properties, transactionMonitorFlavor);

        launchPostgresCDC(hazelcastInstance, postgresProperties,
                Objects.toString(properties.get(MyConstants.PROJECT_PROVENANCE)));

        logStuff(hazelcastInstance);
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
     * @param useHzCloud
     * @param cloudOrHzCloud
     */
    private static void logUseHzCloud(boolean useHzCloud, String cloudOrHzCloud) {
        if (useHzCloud) {
            LOGGER.info("Using Hazelcast Cloud = '{}'=='{}'", MyConstants.USE_HZ_CLOUD, cloudOrHzCloud);
        } else {
            LOGGER.info("Using Non-Hazelcast Cloud = '{}'=='{}'", MyConstants.USE_HZ_CLOUD, cloudOrHzCloud);
        }
    }

    /**
     * <p>Use SQL to copy alerts to Kafka outbound topic.
     * </p>
     *
     * @param hazelcastInstance
     * @param bootstrapServers
     */
    private static void launchAlertsToKafkaAndToLog(HazelcastInstance hazelcastInstance, String bootstrapServers,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        String topic = MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_ALERTS;

        String sqlJobKafkaToMap =
                "CREATE JOB IF NOT EXISTS \"" + MyConstants.SQL_JOB_NAME_KAFKA_TO_IMAP + "\""
                + " AS "
                + " SINK INTO \"" + MyConstants.IMAP_NAME_AUDIT_LOG + "\""
                + " SELECT * FROM \"" + topic + "\"";

        String concatenation;
        String xxx = "UNMERGED_INTO_5.3_";
        LOGGER.error("Reminder to remove job prefix {}", xxx);
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            concatenation = "code";
            break;
        case TRADE:
        default:
            concatenation = "symbol";
            break;
        }
        String sqlJobMapToKafka =
                "CREATE JOB IF NOT EXISTS \"" + xxx + MyConstants.SQL_JOB_NAME_IMAP_TO_KAFKA + "\""
                + " AS "
                + " SINK INTO \"" + MyConstants.KAFKA_TOPIC_NAME_ALERTS + "\""
                + " SELECT __key, " + concatenation + " || ',' || provenance || ',' || whence || ',' || volume"
                + " FROM \"" + MyConstants.IMAP_NAME_ALERTS_LOG + "\"";

        //FIXME 5.2 Style, to be removed once "sqlJobMapToKafka" runs as streaming in 5.3
        //FIXME https://docs.hazelcast.com/hazelcast/5.3-snapshot/sql/querying-maps-sql#streaming-map-changes
        try {
            Pipeline pipelineAlertingToKafka = AlertingToKafka.buildPipeline(bootstrapServers);

            JobConfig jobConfigAlertingToKafka = new JobConfig();
            jobConfigAlertingToKafka.setName(AlertingToKafka.class.getSimpleName());
            jobConfigAlertingToKafka.addClass(HazelcastJsonValueSerializer.class);

            UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAlertingToKafka, jobConfigAlertingToKafka);
        } catch (Exception e) {
            LOGGER.error("launchAlertsSqlToKafka:", e);
        }

        //FIXME Submit "sqlJobMapToKafka", will complete until ready for streaming as reminder
        for (String sql : List.of(sqlJobKafkaToMap, sqlJobMapToKafka)) {
            try {
                hazelcastInstance.getSql().execute(sql);
                LOGGER.info("SQL running: '{}'", sql);
            } catch (Exception e) {
                LOGGER.error("launchAlertsSqlToKafka:" + sql, e);
            }
        }
    }

    /**
     * <p>Launch Slack jobs for SQL (read/write) and alerting (write)
     * </p>
     *
     * @param useHzCloud
     * @param projectName
     * @param hazelcastInstance
     * @param properties
     */
    private static void launchSlackReadWrite(boolean useHzCloud, Object projectName,
            HazelcastInstance hazelcastInstance, Properties properties,
            TransactionMonitorFlavor transactionMonitorFlavor) {

        try {
            UtilsSlackSQLJob.submitJob(hazelcastInstance,
                    projectName == null ? "" : projectName.toString());
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
        }

        // Slack alerting (writing), indirectly uses common utils
        if (useHzCloud) {
            //TODO Fix once supported by HZ Cloud
            LOGGER.error("Slack is not currently supported on Hazelcast Cloud");
        } else {
            launchSlackJob(hazelcastInstance, properties, transactionMonitorFlavor);
        }

    }

    /**
     * <p>Optional, but really cool, job for integration with Slack.
     * </p>
     * @param hazelcastInstance
     * @param properties
     */
    private static void launchSlackJob(HazelcastInstance hazelcastInstance, Properties properties,
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

            LOGGER.info("Job - {}",
                UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAlertingToSlack, jobConfigAlertingToSlack)
            );
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + AlertingToSlack.class.getSimpleName(), e);
        }
    }

    /**
     * <p>Launch a job to read changes from Postgres (that you can make
     * by directly connecting) into Hazelcast
     * </p>
     *
     * @param hazelcastInstance
     * @param properties
     */
    private static void launchPostgresCDC(HazelcastInstance hazelcastInstance,
            Properties properties, String ourProjectProvenance) {

        try {
            Pipeline pipelinePostgresCDC = PostgresCDC.buildPipeline(
                    Objects.toString(properties.get(MyConstants.POSTGRES_ADDRESS)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_DATABASE)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_SCHEMA)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_USER)),
                    Objects.toString(properties.get(MyConstants.POSTGRES_PASSWORD)),
                    hazelcastInstance.getConfig().getClusterName(),
                    MyConstants.IMAP_NAME_ALERTS_LOG,
                    ourProjectProvenance
                    );

            JobConfig jobConfigPostgresCDC = new JobConfig();
            jobConfigPostgresCDC.setName(PostgresCDC.class.getSimpleName());
            jobConfigPostgresCDC.addClass(PostgresCDC.class);

            LOGGER.info("Job - {}",
                UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelinePostgresCDC, jobConfigPostgresCDC)
            );
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + PostgresCDC.class.getSimpleName(), e);
        }

    }

    public static void logStuff(HazelcastInstance hazelcastInstance) {
        logJobs(hazelcastInstance);
        logMaps(hazelcastInstance);
    }

    /**
     * <p>Confirm the running jobs to the console.
     * </p>
     */
    private static void logJobs(HazelcastInstance hazelcastInstance) {
        LOGGER.info("~_~_~_~_~");
        LOGGER.info("logJobs()");
        LOGGER.info("---------");
        hazelcastInstance.getJet().getJobs().forEach(job -> {
            try {
                LOGGER.info("Job name '{}', id {}, status {}, submission {} ({})",
                    Objects.toString(job.getName()), job.getId(), job.getStatus(),
                    job.getSubmissionTime(), new Date(job.getSubmissionTime()));
                JobConfig jobConfig = job.getConfig();
                Object originalSql =
                        jobConfig.getArgument(JobConfigArguments.KEY_SQL_QUERY_TEXT);
                if (originalSql != null) {
                    LOGGER.info(" Original SQL: {}", originalSql);
                }
            } catch (Exception e) {
                String message = String.format("logJobs(): %s", Objects.toString(job));
                LOGGER.info(message, e);
            }
        });
        LOGGER.info("---------");
        LOGGER.info("~_~_~_~_~");
    }

    /**
     * <p>Confirm the maps sizes to the console.
     * </p>
     */
    private static void logMaps(HazelcastInstance hazelcastInstance) {
        Set<String> iMapNames = hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> distributedObject instanceof IMap)
                .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .map(distributedObject -> distributedObject.getName())
                .collect(Collectors.toCollection(TreeSet::new));

        LOGGER.info("~_~_~_~_~");
        LOGGER.info("logMaps()");
        LOGGER.info("---------");
        for (String iMapName : iMapNames) {
            LOGGER.info("IMap: name '{}', size {}",
                    iMapName, hazelcastInstance.getMap(iMapName).size());
        }
        LOGGER.info("---------");
        LOGGER.info("~_~_~_~_~");
    }
}
