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

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>Starts at most one job with the given name.
 * </p>
 */
public class JobControlStartRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobControlStopRunnable.class);

    private final HazelcastInstance hazelcastInstance;
    private final String targetJobNamePrefix;
    private final String bootstrapServers;

    public JobControlStartRunnable(String arg0, String arg1) {
        this.hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
        this.targetJobNamePrefix = arg0;
        this.bootstrapServers = arg1;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        // Check for duplicate
        List<Job> jobs = this.hazelcastInstance.getJet().getJobs();
        for (Job job : jobs) {
            // Name may be null
            String jobName = Objects.toString(job.getName());
            String jobNamePrefix = jobName.split("@")[0];
            if (jobNamePrefix.equals(targetJobNamePrefix)) {
                JobStatus jobStatus = job.getStatus();
                if ((jobStatus == JobStatus.RUNNING)) {
                    LOGGER.error("START job with prefix '{}', ignoring due to presence of '{}'",
                            this.targetJobNamePrefix, job);
                } else {
                    LOGGER.debug("START job with prefix '{}', ignoring '{}' due to status", this.targetJobNamePrefix, job);
                }
            }
        }

        // Actually launch
        if (targetJobNamePrefix.equals(IngestTrades.class.getSimpleName())) {
            // Trade ingest
            Pipeline pipelineIngestTrades = IngestTrades.buildPipeline(this.bootstrapServers);

            JobConfig jobConfigIngestTrades = new JobConfig();
            jobConfigIngestTrades.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigIngestTrades.setName(IngestTrades.class.getSimpleName() + "@" + now);

            try {
                hazelcastInstance.getJet().newJob(pipelineIngestTrades, jobConfigIngestTrades);
            } catch (Exception e) {
                LOGGER.error("Failed issuing start for " + targetJobNamePrefix, e);
            }
        } else {
            if (targetJobNamePrefix.equals(AggregateQuery.class.getSimpleName())) {
                // Trade aggregation
                Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(this.bootstrapServers);

                JobConfig jobConfigAggregateQuery = new JobConfig();
                jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
                jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName() + "@" + now);
                jobConfigAggregateQuery.addClass(MaxVolumeAggregator.class);

                try {
                    hazelcastInstance.getJet().newJob(pipelineAggregateQuery, jobConfigAggregateQuery);
                } catch (Exception e) {
                    LOGGER.error("Failed issuing start for " + targetJobNamePrefix, e);
                }
            } else {
                LOGGER.error("Failed issuing start for '{}', unknown job", targetJobNamePrefix);
            }
        }
    }
}
