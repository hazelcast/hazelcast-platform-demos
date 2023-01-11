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
    private final String pulsarList;
    private final boolean usePulsar;
    private final String projectName;
    private final String clusterName;
    private final TransactionMonitorFlavor transactionMonitorFlavor;

    public JobControlStartRunnable(String arg0, String arg1, String arg2, boolean arg3,
            String arg4, String arg5, TransactionMonitorFlavor arg6) {
        this.hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
        this.targetJobNamePrefix = arg0;
        this.bootstrapServers = arg1;
        this.pulsarList = arg2;
        this.usePulsar = arg3;
        this.projectName = arg4;
        this.clusterName = arg5;
        this.transactionMonitorFlavor = arg6;
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
        if (targetJobNamePrefix.equals(IngestTransactions.class.getSimpleName())) {
            // Transaction ingest
            Pipeline pipelineIngestTransactions = IngestTransactions.buildPipeline(this.bootstrapServers,
                this.pulsarList, this.usePulsar, this.transactionMonitorFlavor);

            JobConfig jobConfigIngestTransactions = new JobConfig();
            jobConfigIngestTransactions.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
            jobConfigIngestTransactions.setName(IngestTransactions.class.getSimpleName() + "@" + now);

            try {
                hazelcastInstance.getJet().newJob(pipelineIngestTransactions, jobConfigIngestTransactions);
            } catch (Exception e) {
                LOGGER.error("Failed issuing start for " + targetJobNamePrefix, e);
            }
        } else {
            if (targetJobNamePrefix.equals(AggregateQuery.class.getSimpleName())) {
                // Transaction aggregation
                JobConfig jobConfigAggregateQuery = new JobConfig();
                jobConfigAggregateQuery.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
                jobConfigAggregateQuery.setName(AggregateQuery.class.getSimpleName() + "@" + now);
                jobConfigAggregateQuery.addClass(MaxAggregator.class);

                Pipeline pipelineAggregateQuery = AggregateQuery.buildPipeline(this.bootstrapServers,
                        this.pulsarList, this.usePulsar, projectName, jobConfigAggregateQuery.getName(),
                        this.clusterName, this.transactionMonitorFlavor);

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
