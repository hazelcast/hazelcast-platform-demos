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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.platform.demos.banking.cva.MyUtils;

/**
 * <p>Submit the CVA job if the previous run has finished.
 * </p>
 */
public class CvaStpJobSubmitter {

    /**
     * <p>Submit the {@link CvaStpJob} so that one is running.
     * </p>
     * <p>The job may potentially generate billions of intermediate results,
     * so not be instant. But as submitted by the end user, one run may be
     * asked for before the previous one has finished -- don't allow this.
     * Determine this using the job name as a prefix, and the timestamp as
     * a suffix.
     * </p>
     *
     * @param debug If debug job steps are required
     * @param jetInstance Used to find similar named jobs
     * @return The job if submitted
     * @throws Exception If the job is rejected as a duplicate is still running
     */
    public static Job submitCvaStpJob(boolean debug, JetInstance jetInstance) throws Exception {
        long timestamp = System.currentTimeMillis();
        String timestampStr = MyUtils.timestampToISO8601(timestamp);

        Pipeline pipeline = CvaStpJob.buildPipeline(timestampStr, debug);
        String jobNamePrefix = CvaStpJob.class.getSimpleName();

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(jobNamePrefix + "-" + timestampStr);
        jobConfig.addClass(CvaStpJob.class);
        
        Job job = MyUtils.findRunningJobsWithSamePrefix(jobNamePrefix, jetInstance);
        if (job != null) {
            String message = String.format("Previous job '%s' id=='{}' still at status '%s'",
                    job.getName(), job.getId(), job.getStatus());
            throw new RuntimeException(message);
        } else {
            return jetInstance.newJobIfAbsent(pipeline, jobConfig);
        }
    }

}
