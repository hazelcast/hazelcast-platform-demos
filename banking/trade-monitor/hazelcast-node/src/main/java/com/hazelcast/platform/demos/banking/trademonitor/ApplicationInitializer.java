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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.IMap;


/**
 * <p>Initialise the Jet cluster to ensure the necessary extra parts
 * exist and necessary jobs are running. These are idempotent operations,
 * the will only do anything for the first node in the Jet cluster.
 * </p>
 */
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);

    /**
     * <p>Ensure the necessary {@link com.hazelcast.core.DistributedObject} exist to
     * hold processing results. Launch the Jet jobs for this example.
     * </p>
     */
    public static void initialise(JetInstance jetInstance, String bootstrapServers) throws Exception {
        createNeededObjects(jetInstance);
        loadNeededData(jetInstance);
        defineQueryableObjects(bootstrapServers, jetInstance);
        launchNeededJobs(jetInstance, bootstrapServers);
    }


    /**
     * <p>Access the {@link com.hazelcast.map.IMap} and other objects
     * that are used by the example. This will create them on first
     * access, so ensuring all are visible from the outset.
     * </p>
     */
    static void createNeededObjects(JetInstance jetInstance) {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            jetInstance.getHazelcastInstance().getMap(iMapName);
        }
    }


    /**
     * <p>Stock symbols are needed for trade look-up enrichment,
     * the first member to start loads them from a file into
     * a {@link com.hazelcast.map.IMap}.
     * </p>
     */
    static void loadNeededData(JetInstance jetInstance) throws Exception {
        IMap<String, SymbolInfo> symbolsMap =
                jetInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);

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
    }


    /**
     * <p>Define Hazelcast maps &amp; Kafka topics for later SQL querying.
     * </p>
     */
    static void defineQueryableObjects(String bootstrapServers, JetInstance jetInstance) {
        defineKafka(bootstrapServers, jetInstance);
        defineIMap(jetInstance);
    }


    /**
     * <p>Define Kafka streams so can be directly used as a
     * querying source by SQL.
     * </p>
     *
     * @param bootstrapServers
     */
    static void defineKafka(String bootstrapServers, JetInstance jetInstance) {
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
                + " 'valueFormat' = 'json',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        define(definition1, jetInstance);
    }


    /**
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     *
     * @param jetInstance
     */
    static void defineIMap(JetInstance jetInstance) {
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
                + MyConstants.IMAP_NAME_SYMBOLS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + SymbolInfo.class.getCanonicalName() + "'"
                + " )";

        String definition3 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRADES
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Trade.class.getCanonicalName() + "'"
                + " )";

        define(definition1, jetInstance);
        define(definition2, jetInstance);
        define(definition3, jetInstance);
    }


    /**
     * <p>Generic handler to loading definitions
     * </p>
     *
     * @param definition
     * @param jetInstance
     */
    static void define(String definition, JetInstance jetInstance) {
        LOGGER.trace("Definition '{}'", definition);
        try {
            jetInstance.getSql().execute(definition);
        } catch (Exception e) {
            LOGGER.error(definition, e);
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
     */
    static void launchNeededJobs(JetInstance jetInstance, String bootstrapServers) {
        // Trade ingest
        Pipeline pipelineIngestTrades = IngestTrades.buildPipeline(bootstrapServers);

        JobConfig jobConfigIngestTrades = new JobConfig();
        jobConfigIngestTrades.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
        jobConfigIngestTrades.setName(IngestTrades.class.getSimpleName());

        jetInstance.newJobIfAbsent(pipelineIngestTrades, jobConfigIngestTrades);

        // Trade aggregation
        Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(bootstrapServers);

        JobConfig jobConfigAggregateQuery = new JobConfig();
        jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
        jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName());

        jetInstance.newJobIfAbsent(pipelineAggregateQuery, jobConfigAggregateQuery);

    }

}
