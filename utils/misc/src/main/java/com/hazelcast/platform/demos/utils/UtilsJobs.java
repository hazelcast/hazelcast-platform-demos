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

package com.hazelcast.platform.demos.utils;

import java.util.List;

import org.slf4j.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>Helpers for job submission.
 * </p>
 */
public class UtilsJobs {

    /**
     * <p>{@link JetService#newJobIfAbsent} won't submit a new job if one is running
     * with the given name, but will if one has failed. If the job exists, even if
     * failed, there's no point trying again.
     * <p>
     * </p>
     *
     * @param logger
     * @param hazelcastInstance
     * @param pipeline
     * @param jobConfig
     */
    public static Job myNewJobIfAbsent(Logger logger, HazelcastInstance hazelcastInstance,
            Pipeline pipeline, JobConfig jobConfig) {
        String jobName = jobConfig.getName();
        List<Job> jobs = hazelcastInstance.getJet().getJobs();
        for (Job job : jobs) {
            if (job.getName() == null) {
                logger.error("Job with null name: {}", job);
            } else {
                if (!job.getName().isBlank() && job.getName().equals(jobName)) {
                    logger.debug("myNewJobIfAbsent: skip '{}', exists with status {}", jobName, job.getStatus());
                    return null;
                }
            }
        }
        try {
            return hazelcastInstance.getJet().newJobIfAbsent(pipeline, jobConfig);
        } catch (Exception e) {
            logger.error("myNewJobIfAbsent:" + jobName, e);
            return null;
        }
    }

}
