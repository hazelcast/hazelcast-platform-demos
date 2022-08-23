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
     * <p>Mappings needed for WAN, rather than replicate "{@code __sql.catalog}"
     * </p>
     */
    public static boolean createMinimalMappings(HazelcastInstance hazelcastInstance) {
        return defineWANIMaps(hazelcastInstance);
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
            Properties postgresProperties, String ourProjectProvenance) {
        // Capture what was present before
        Set<String> existingIMapNames = hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> distributedObject instanceof IMap)
                .map(distributedObject -> distributedObject.getName())
                .filter(name -> !name.startsWith("__"))
                .collect(Collectors.toCollection(TreeSet::new));

        // Add journals and map stores to maps before they are created
        boolean ok = dynamicMapConfig(hazelcastInstance, existingIMapNames,
                postgresProperties, ourProjectProvenance);

        // Accessing non-existing maps does not return any failures
        for (String iMapName : MyConstants.IMAP_NAMES) {
            if (!existingIMapNames.contains(iMapName)) {
                hazelcastInstance.getMap(iMapName);
            }
        }

        // Add index to maps after they are created, if created in this method's run.
        if (ok) {
            ok = defineIndexes(hazelcastInstance, existingIMapNames);
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
     *         class-name: hazelcast.platform.demos.banking.trademonitor.AlertingToPostgresStore
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
            Set<String> existingIMapNames, Properties postgresProperties, String ourProjectProvenance) {
        final String alertsWildcard = "alerts*";

        EventJournalConfig eventJournalConfig = new EventJournalConfig();
        eventJournalConfig.setEnabled(true);

        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME)) {
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

            alertsMapConfig.setMapStoreConfig(mapStoreConfig);

            hazelcastInstance.getConfig().addMapConfig(alertsMapConfig);
        } else {
            LOGGER.trace("Don't add journal to '{}', map already exists", MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME);
        }

        return true;
    }

    /**
     * <p>Maps that have indexes, currently just the Trades made for
     * faster searching. When created manually it would be:
     * <pre>
     *     'trades':
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
    private static boolean defineIndexes(HazelcastInstance hazelcastInstance, Set<String> existingIMapNames) {

        // Only add if map hadn't previously existed and so has just been created
        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_TRADES)) {
            IMap<?, ?> tradesMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_TRADES);

            IndexConfig indexConfig = new IndexConfig();
            indexConfig.setName(MyConstants.IMAP_NAME_TRADES + "_idx");
            indexConfig.setType(IndexType.HASH);
            indexConfig.setAttributes(Arrays.asList("symbol"));

            // Void method, hence returning true
            tradesMap.addIndex(indexConfig);
        } else {
            LOGGER.trace("Don't add index to '{}', map already exists", MyConstants.IMAP_NAME_TRADES);
        }

        return true;
    }

    /**
     * <p>Kafka properties can be stashed for ad-hoc jobs to use.
     * </p>
     * <p>Stock symbols are needed for trade look-up enrichment,
     * the first member to start loads them from a file into
     * a {@link com.hazelcast.map.IMap}.
     * </p>
     */
    public static boolean loadNeededData(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarList, boolean usePulsar, boolean useHzCloud) {
        boolean ok = true;
        try {
            IMap<String, String> jobConfigMap =
                    hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONFIG);
            IMap<String, SymbolInfo> symbolsMap =
                    hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);

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
        } catch (Exception e) {
            LOGGER.error("loadNeededData()", e);
            ok = false;
        }
        return ok;
    }

    /**
     * <p>Define Hazelcast maps &amp; Kafka topics for later SQL querying.
     * </p>
     */
    public static boolean defineQueryableObjects(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        boolean ok = true;
        ok &= defineKafka(hazelcastInstance, bootstrapServers);
        ok &= defineWANIMaps(hazelcastInstance);
        ok &= defineIMaps1(hazelcastInstance);
        ok &= defineIMaps2(hazelcastInstance);
        ok &= defineIMaps3(hazelcastInstance);
        return ok;
    }


    /**
     * <p>Define Kafka streams so can be directly used as a
     * querying source by SQL.
     * </p>
     *
     * @param bootstrapServers
     */
    static boolean defineKafka(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        String definition1 = "CREATE EXTERNAL MAPPING IF NOT EXISTS "
                // Name for our SQL
                + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRADES
                // Name of the remote object
                + " EXTERNAL NAME " + MyConstants.KAFKA_TOPIC_NAME_TRADES
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
        ok = define(definition1, hazelcastInstance);
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
    static boolean defineWANIMaps(HazelcastInstance hazelcastInstance) {
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

        String definition5 = "CREATE MAPPING IF NOT EXISTS "
                 + MyConstants.IMAP_NAME_SYMBOLS
                 + " TYPE IMap "
                 + " OPTIONS ( "
                 + " 'keyFormat' = 'java',"
                 + " 'keyJavaClass' = 'java.lang.String',"
                 + " 'valueFormat' = 'java',"
                 + " 'valueJavaClass' = '" + SymbolInfo.class.getName() + "'"
                 + " )";

        boolean ok = true;
        List<String> definitions = List.of(definition3, definition4, definition5);
        for (String definition : definitions) {
            ok &= define(definition, hazelcastInstance);
        }
        if (definitions.size() != MyConstants.WAN_IMAP_NAMES.size()) {
            LOGGER.error("Not all WAN maps defined");
            return false;
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
    static boolean defineIMaps1(HazelcastInstance hazelcastInstance) {
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
                + MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME
                + " ("
                + "    __key BIGINT,"
                + "    symbol VARCHAR,"
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
    static boolean defineIMaps2(HazelcastInstance hazelcastInstance) {
        String definition8 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_PORTFOLIOS
                + " ("
                + "    __key VARCHAR,"
                + "    stock VARCHAR,"
                + "    sold INTEGER,"
                + "    bought INTEGER,"
                + "    change INTEGER"
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getName() + "',"
                + " 'valueFormat' = 'compact',"
                + " 'valueCompactTypeName' = '" + Portfolio.class.getSimpleName() + "'"
                + " )";

        String definition9 = "CREATE MAPPING IF NOT EXISTS "
                 + MyConstants.IMAP_NAME_TRADES
                 + " TYPE IMap "
                 + " OPTIONS ( "
                 + " 'keyFormat' = 'java',"
                 + " 'keyJavaClass' = 'java.lang.String',"
                 + " 'valueFormat' = 'java',"
                 + " 'valueJavaClass' = '" + Trade.class.getName() + "'"
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

        String definition11 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_JOB_CONTROL
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
                + " )";

        boolean ok = define(definition8, hazelcastInstance);
        ok &= define(definition9, hazelcastInstance);
        ok &= define(definition10, hazelcastInstance);
        ok &= define(definition11, hazelcastInstance);
        return ok;
    }

    /**
     * <p>Even more map definitions
     * </p>
     * @param hazelcastInstance
     */
    static boolean defineIMaps3(HazelcastInstance hazelcastInstance) {
        // Not much of view, but shows the concept
        String definition12 =  "CREATE OR REPLACE VIEW "
                + MyConstants.IMAP_NAME_TRADES + MyConstants.VIEW_SUFFIX
                + " AS SELECT "
                + "    __key"
                + "      AS \"primary_key\""
                + " FROM " + MyConstants.IMAP_NAME_TRADES;

        boolean ok = true;
        ok &= define(definition12, hazelcastInstance);
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
     * <p><i>1</i> Launch a job to read trades from Kafka and place them in a map,
     * a simple upload.
     * </p>
     * <p><i>2</i> Launch a job to read the same trades from Kafka and to aggregate
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
            String pulsarList, Properties postgresProperties, Properties properties, String clusterName) {
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

            // Trade ingest
            Pipeline pipelineIngestTrades = IngestTrades.buildPipeline(bootstrapServers, pulsarList, usePulsar);

            JobConfig jobConfigIngestTrades = new JobConfig();
            jobConfigIngestTrades.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigIngestTrades.setName(IngestTrades.class.getSimpleName());
            jobConfigIngestTrades.addClass(IngestTrades.class);

            if (usePulsar && useHzCloud) {
                //TODO Fix once supported by HZ Cloud
                LOGGER.error("Pulsar is not currently supported on Hazelcast Cloud");
            } else {
                UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineIngestTrades, jobConfigIngestTrades);
            }

            // Trade aggregation
            JobConfig jobConfigAggregateQuery = new JobConfig();
            jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName());
            jobConfigAggregateQuery.addClass(AggregateQuery.class);
            jobConfigAggregateQuery.addClass(MaxVolumeAggregator.class);
            jobConfigAggregateQuery.addClass(UtilsFormatter.class);

            Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(bootstrapServers,
                    pulsarList, usePulsar, projectName, jobConfigAggregateQuery.getName(),
                    clusterName);

            if (usePulsar && useHzCloud) {
                //TODO Fix once supported by HZ Cloud
                LOGGER.error("Pulsar is not currently supported on Hazelcast Cloud");
            } else {
                UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAggregateQuery, jobConfigAggregateQuery);
                // Aggregate query creates alerts to an IMap. Use a separate rather than same job to copy to Kafka.
                launchAlertsToKafkaAndToLog(hazelcastInstance, bootstrapServers);
            }

        }

        // Remaining jobs need properties
        if (properties.size() == 0) {
            LOGGER.error("launchNeededJobs: properties is empty");
            return false;
        }

        // Slack SQL integration (reading/writing) from common utils
        launchSlackReadWrite(useHzCloud, projectName, hazelcastInstance, properties);

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
    private static void launchAlertsToKafkaAndToLog(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        String topic = MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_ALERTS;

        /*FIXME 5.3. Make both jobs streaming SQL
        String sqlJobMapToKafka = "SINK INTO "
                + MyConstants.KAFKA_TOPIC_NAME_ALERTS
                + " SELECT __key, symbol || ',' || provenance || ',' || whence || ',' || volume FROM "
                + MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME;
                */

        String sqlJobKafkaToMap =
                "CREATE JOB IF NOT EXISTS " + MyConstants.SQL_JOB_NAME_KAFKA_TO_IMAP
                + " AS "
                + " SINK INTO " + MyConstants.IMAP_NAME_AUDIT_LOG
                + " SELECT * FROM " + topic;

        // 5.2 Style, to be removed in favor of SQL for both.
        try {
            Pipeline pipelineAlertingToKafka = AlertingToKafka.buildPipeline(bootstrapServers);

            JobConfig jobConfigAlertingToKafka = new JobConfig();
            jobConfigAlertingToKafka.setName(AlertingToKafka.class.getSimpleName());
            jobConfigAlertingToKafka.addClass(HazelcastJsonValueSerializer.class);

            UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAlertingToKafka, jobConfigAlertingToKafka);
        } catch (Exception e) {
            LOGGER.error("launchAlertsSqlToKafka:", e);
        }

        for (String sql : List.of(sqlJobKafkaToMap)) {
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
            HazelcastInstance hazelcastInstance, Properties properties) {

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
            launchSlackJob(hazelcastInstance, properties);
        }

    }

    /**
     * <p>Optional, but really cool, job for integration with Slack.
     * </p>
     * @param hazelcastInstance
     * @param properties
     */
    private static void launchSlackJob(HazelcastInstance hazelcastInstance, Properties properties) {
        try {
            Pipeline pipelineAlertingToSlack = AlertingToSlack.buildPipeline(
                    properties.get(UtilsConstants.SLACK_ACCESS_TOKEN),
                    properties.get(UtilsConstants.SLACK_CHANNEL_NAME),
                    properties.get(UtilsConstants.SLACK_PROJECT_NAME),
                    properties.get(UtilsConstants.SLACK_BUILD_USER)
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
                    MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME,
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

    private static void logStuff(HazelcastInstance hazelcastInstance) {
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
            LOGGER.info("Job name '{}', id {}, status {}, submission {} ({})",
                    Objects.toString(job.getName()), job.getId(), job.getStatus(),
                    job.getSubmissionTime(), new Date(job.getSubmissionTime()));
            try {
                JobConfig jobConfig = job.getConfig();
                Object originalSql =
                        jobConfig.getArgument(JobConfigArguments.KEY_SQL_QUERY_TEXT);
                if (originalSql != null) {
                    LOGGER.info(" Original SQL: {}", originalSql);
                }
            } catch (Exception e) {
                LOGGER.info("JobConfig", e);
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
