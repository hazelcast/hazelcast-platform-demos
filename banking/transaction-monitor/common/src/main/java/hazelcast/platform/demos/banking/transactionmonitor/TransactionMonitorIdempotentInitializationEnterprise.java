/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.platform.demos.utils.UtilsJobs;

/**
 * <p>Enterprise specific initialization.
 * </p>
 */
public class TransactionMonitorIdempotentInitializationEnterprise {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMonitorIdempotentInitializationEnterprise.class);

    private static final long TEN_SECONDS_MS = 10_000L;

    /**
     * <p>Launch all enterprise-only jobs.
     * </p>
     *
     * @param hazelcastInstance
     * @param useHzCloud
     * @return
     */
    public static boolean launchNeededEnterpriseJobs(HazelcastInstance hazelcastInstance, boolean useHzCloud,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        boolean ok = true;
        ok &= doVectorCollectionMomentsJob(hazelcastInstance, transactionMonitorFlavor);
        return ok;
    }

    /**
     * <p>Launch a job that writes into a vector collection for online browsing
     * with the "{@code vector-search}" module of this demo.
     * </p>
     *
     * @param hazelcastInstance
     */
    private static boolean doVectorCollectionMomentsJob(HazelcastInstance hazelcastInstance,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        Pipeline vectorCollectionMomentsPipeline = VectorCollectionMoments.getPipeline(transactionMonitorFlavor);
        JobConfig vectorCollectionMomentsJobConfig = VectorCollectionMoments.getJobConfig();

        try {
            long now = System.currentTimeMillis();
            Job job = UtilsJobs.myNewJobIfAbsent(LOGGER,
                    hazelcastInstance,
                    vectorCollectionMomentsPipeline, vectorCollectionMomentsJobConfig);

            if (job == null) {
                LOGGER.error("doVectorCollectionMomentsJob: job is null");
                return false;
            }

            if (job.getSubmissionTime() < (now - TEN_SECONDS_MS)) {
                LOGGER.error("doVectorCollectionMomentsJob: job exists: {}", job);
                return false;
            } else {
                LOGGER.info("doVectorCollectionMomentsJob: job launched: {}", job);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("doVectorCollectionMomentsJob", e);
            return false;
        }
    }

}
