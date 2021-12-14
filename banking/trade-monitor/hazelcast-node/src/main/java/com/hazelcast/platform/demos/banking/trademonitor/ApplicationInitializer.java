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

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.platform.demos.utils.UtilsConstants;
import com.hazelcast.platform.demos.utils.UtilsProperties;
import com.hazelcast.platform.demos.utils.UtilsSlack;
import com.hazelcast.platform.demos.utils.UtilsSlackSQLJob;
import com.hazelcast.platform.demos.utils.UtilsSlackSink;

import hazelcast.platform.demos.banking.trademonitor.MyConstants;


/**
 * <p>Initialise the Jet cluster to ensure the necessary extra parts
 * exist and necessary jobs are running. These are idempotent operations,
 * the will only do anything for the first node in the Jet cluster.
 * </p>
 */
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);
    // Local constant, never needed outside this class
    private static final String APPLICATION_PROPERTIES_FILE = "application.properties";

    /**
     * <p>Ensure the necessary {@link com.hazelcast.core.DistributedObject} exist to
     * hold processing results. Launch the Jet jobs for this example.
     * </p>
     */
    public static void initialise(HazelcastInstance hazelcastInstance, String bootstrapServers) throws Exception {
        addListeners(hazelcastInstance, bootstrapServers);
        CommonIdempotentInitialization.createNeededObjects(hazelcastInstance);
        CommonIdempotentInitialization.loadNeededData(hazelcastInstance, bootstrapServers);
        CommonIdempotentInitialization.defineQueryableObjects(hazelcastInstance, bootstrapServers);
        launchNeededJobs(hazelcastInstance, bootstrapServers);
    }


    /**
     * <p>Logging listeners, and job control.
     * </p>
     *
     * @param hazelcastInstance
     */
    static void addListeners(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        MyMembershipListener myMembershipListener = new MyMembershipListener(hazelcastInstance);
        hazelcastInstance.getCluster().addMembershipListener(myMembershipListener);

        JobControlListener jobControlListener = new JobControlListener(bootstrapServers);
        hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONTROL)
            .addLocalEntryListener(jobControlListener);
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
    static void launchNeededJobs(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        // Only do this for the first node.
        if (hazelcastInstance.getCluster().getMembers().size() != 1) {
            return;
        }

        if (!System.getProperty("my.autostart.enabled", "").equalsIgnoreCase("true")) {
            LOGGER.info("Not launching Kafka jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));
        } else {
            LOGGER.info("Launching Kafka jobs automatically at cluster creation: 'my.autostart.enabled'=='{}'",
                    System.getProperty("my.autostart.enabled"));

            // Trade ingest
            Pipeline pipelineIngestTrades = IngestTrades.buildPipeline(bootstrapServers);

            JobConfig jobConfigIngestTrades = new JobConfig();
            jobConfigIngestTrades.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigIngestTrades.setName(IngestTrades.class.getSimpleName());

            hazelcastInstance.getJet().newJobIfAbsent(pipelineIngestTrades, jobConfigIngestTrades);

            // Trade aggregation
            Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(bootstrapServers);

            JobConfig jobConfigAggregateQuery = new JobConfig();
            jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName());
            jobConfigAggregateQuery.addClass(MaxVolumeAggregator.class);

            hazelcastInstance.getJet().newJobIfAbsent(pipelineAggregateQuery, jobConfigAggregateQuery);
        }

        // Remaining jobs need properties, skip if not as expected

        Properties properties = null;
        try {
            properties = UtilsProperties.loadClasspathProperties(APPLICATION_PROPERTIES_FILE);
            Properties properties2 = UtilsSlack.loadSlackAccessProperties();
            properties.putAll(properties2);
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName()
                    + " - No jobs submitted for Slack");
            return;
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
        try {
            Pipeline pipelineAlertingToSlack = AlertingToSlack.buildPipeline(
                    properties.get(UtilsConstants.SLACK_ACCESS_TOKEN),
                    properties.get(UtilsConstants.SLACK_CHANNEL_NAME),
                    properties.get(UtilsConstants.SLACK_PROJECT_NAME)
                    );

            JobConfig jobConfigAlertingToSlack = new JobConfig();
            jobConfigAlertingToSlack.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigAlertingToSlack.setName(AlertingToSlack.class.getSimpleName());
            jobConfigAlertingToSlack.addClass(AlertingToSlack.class);
            jobConfigAlertingToSlack.addClass(UtilsSlackSink.class);

            hazelcastInstance.getJet().newJobIfAbsent(pipelineAlertingToSlack, jobConfigAlertingToSlack);
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + AlertingToSlack.class.getSimpleName(), e);
        }
    }

}
