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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
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
        IMap<String, String> symbolsMap = jetInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);

        if (!symbolsMap.isEmpty()) {
            LOGGER.trace("Skip loading '{}', not empty", symbolsMap.getName());
        } else {
            Map<String, String> localMap = MyUtils.nasdaqListed();

            symbolsMap.putAll(localMap);

            LOGGER.trace("Loaded {} into '{}'", localMap.size(), symbolsMap.getName());
        }
    }

    /**
     * <p>Launch a pipeline to read trades from Kafka, which needs the Kafka host names
     * and ports for the broker servers.
     * </p>
     */
    static void launchNeededJobs(JetInstance jetInstance, String bootstrapServers) {
        Pipeline pipelineIngestTrades = IngestTrades.buildPipeline(bootstrapServers);

        JobConfig jobConfigIngestTrades = new JobConfig();
        jobConfigIngestTrades.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
        jobConfigIngestTrades.setName(IngestTrades.class.getSimpleName());

        jetInstance.newJobIfAbsent(pipelineIngestTrades, jobConfigIngestTrades);
    }

}
