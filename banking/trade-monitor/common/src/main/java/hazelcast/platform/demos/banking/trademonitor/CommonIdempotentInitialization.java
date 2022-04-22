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
import java.util.Map;
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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.config.JobConfig;
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
    public static boolean createNeededObjects(HazelcastInstance hazelcastInstance) {
        // Capture what was present before
        Set<String> existingIMapNames = hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> distributedObject instanceof IMap)
                .map(distributedObject -> distributedObject.getName())
                .filter(name -> !name.startsWith("__"))
                .collect(Collectors.toCollection(TreeSet::new));

        // Add journals to maps before they are created
        boolean ok = defineJournals(hazelcastInstance, existingIMapNames);

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
     * <p>Add journal configuration to maps that need them. Equivalent to:
     * <pre>
     *     'alerts*':
     *       event-journal:
     *         enabled: true
     * </pre>
     * <p>
     *
     * @param hazelcastInstance
     * @param existingIMapNames - maps that this run of the initialiser didn't create
     * @return true, always, either added or not needed
     */
    private static boolean defineJournals(HazelcastInstance hazelcastInstance, Set<String> existingIMapNames) {
        final String alertsWildcard = "alerts*";

        EventJournalConfig eventJournalConfig = new EventJournalConfig();
        eventJournalConfig.setEnabled(true);

        if (!existingIMapNames.contains(MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME)) {
            MapConfig alertsMapConfig = new MapConfig(alertsWildcard);
            alertsMapConfig.setEventJournalConfig(eventJournalConfig);

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
        ok &= defineIMap(hazelcastInstance);
        ok &= defineIMap2(hazelcastInstance);
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
                //XXX + "    \"timestamp\" VARCHAR,"
                //XXX + "    symbol VARCHAR,"
                //XXX + "    volume BIGINT"
                + "    this VARCHAR"
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.Long',"
                //XXX+ " 'valueFormat' = 'json-flat',"
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
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     *
     * @param hazelcastInstance
     */
    static boolean defineIMap(HazelcastInstance hazelcastInstance) {
        String definition1 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple3.class.getCanonicalName() + "'"
                + " )";

        String definition2 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME
                + " ("
                + "    __key BIGINT,"
                + "    \"timestamp\" VARCHAR,"
                + "    symbol VARCHAR,"
                + "    volume BIGINT"
                + ")"
                 + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.Long',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";

        String definition3 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_JOB_CONFIG
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + String.class.getCanonicalName() + "'"
                + " )";

        String definition4 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_SYMBOLS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + SymbolInfo.class.getCanonicalName() + "'"
                + " )";

        String definition5 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRADES
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Trade.class.getCanonicalName() + "'"
                + " )";

        boolean ok = true;
        ok &= define(definition1, hazelcastInstance);
        ok &= define(definition2, hazelcastInstance);
        ok &= define(definition3, hazelcastInstance);
        ok &= define(definition4, hazelcastInstance);
        ok &= define(definition5, hazelcastInstance);
        return ok;
    }

    /**
     * <p>More map definitions
     * </p>
     * @param hazelcastInstance
     */
     static boolean defineIMap2(HazelcastInstance hazelcastInstance) {
        String definition6 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_PYTHON_SENTIMENT
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
                + " )";

        String definition7 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_JOB_CONTROL
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
                + " )";

        // Not much of view, but shows the concept
        String definition8 =  "CREATE OR REPLACE VIEW "
                + MyConstants.IMAP_NAME_TRADES + MyConstants.VIEW_SUFFIX
                + " AS SELECT "
                + "    __key"
                + "      AS \"primary_key\""
                + " FROM " + MyConstants.IMAP_NAME_TRADES;

        boolean ok = true;
        ok &= define(definition6, hazelcastInstance);
        ok &= define(definition7, hazelcastInstance);
        ok &= define(definition8, hazelcastInstance);
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
            String pulsarList, Properties properties) {

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
            Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(bootstrapServers, pulsarList, usePulsar);

            JobConfig jobConfigAggregateQuery = new JobConfig();
            jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName());
            jobConfigAggregateQuery.addClass(AggregateQuery.class);
            jobConfigAggregateQuery.addClass(MaxVolumeAggregator.class);
            jobConfigAggregateQuery.addClass(UtilsFormatter.class);

            if (usePulsar && useHzCloud) {
                //TODO Fix once supported by HZ Cloud
                LOGGER.error("Pulsar is not currently supported on Hazelcast Cloud");
            } else {
                UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAggregateQuery, jobConfigAggregateQuery);
                // Aggregate query creates alerts to an IMap. Use a separate rather than same job to copy to Kafka.
                launchAlertsToKafka(hazelcastInstance, bootstrapServers);
            }
        }

        // Remaining jobs need properties
        if (properties.size() == 0) {
            LOGGER.error("launchNeededJobs: properties is empty");
            return false;
        }

        // Slack SQL integration from common utils
        try {
            Object projectName = properties.get(UtilsConstants.SLACK_PROJECT_NAME);

            UtilsSlackSQLJob.submitJob(hazelcastInstance,
                    projectName == null ? "" : projectName.toString());
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
        }

        // Slack alerting, indirectly uses common utils
        if (useHzCloud) {
            //TODO Fix once supported by HZ Cloud
            LOGGER.error("Slack is not currently supported on Hazelcast Cloud");
        } else {
            launchSlackJob(hazelcastInstance, properties);
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
    private static void launchAlertsToKafka(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        String sql = "SINK INTO " + MyConstants.KAFKA_TOPIC_NAME_ALERTS
                + " SELECT * FROM " + MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME;
        try {
            Pipeline pipelineAlertingToKafka = AlertingToKafka.buildPipeline(bootstrapServers);

            JobConfig jobConfigAlertingToKafka = new JobConfig();
            jobConfigAlertingToKafka.setName(AlertingToKafka.class.getSimpleName());
            jobConfigAlertingToKafka.addClass(HazelcastJsonValueSerializer.class);

            UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAlertingToKafka, jobConfigAlertingToKafka);

            //FIXME hazelcastInstance.getSql().execute(sql);
            LOGGER.info("SQL running: '{}'", sql);
        } catch (Exception e) {
            LOGGER.error("launchAlertsSqlToKafka:" + sql, e);
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

}
