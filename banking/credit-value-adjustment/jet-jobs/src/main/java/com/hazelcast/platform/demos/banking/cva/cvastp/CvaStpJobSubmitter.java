/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.banking.cva.MyConstants;
import com.hazelcast.platform.demos.banking.cva.MyUtils;

/**
 * <p>Submit the CVA job if the previous run has finished.
 * </p>
 */
public class CvaStpJobSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CvaStpJobSubmitter.class);

    /**
     * <p>Try to submit the {@link CvaStpJob}, allowing only one to be running
     * per calculation date.
     * </p>
     * <p>Call through to the customer invoker which takes an extra argument
     * for whether to run intermediate debugging stages. These extra stages
     * save output to maps, so slow the job down and the map content could
     * be huge.
     * </p>
     *
     * @param hazelcastInstance Used to find similar named jobs
     * @param calcDate Calculation date to use
     * @return The job if submitted
     * @throws Exception If the job is rejected as a duplicate is still running
     */
    public static Job submitCvaStpJob(HazelcastInstance hazelcastInstance, LocalDate calcDate,
            boolean useViridian) throws Exception {
        boolean debug = false;
        int batchSize = MyConstants.DEFAULT_BATCH_SIZE;
        int parallelism = 1;
        return CvaStpJobSubmitter.submitCvaStpJob(hazelcastInstance, calcDate, batchSize, parallelism,
                debug, useViridian);
    }

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
     * @param hazelcastInstance Used to find similar named jobs
     * @param calcDate Calculation date to use
     * @param batchSize How many calcs to pass to C++
     * @param parallelism How many C++ workers to each each Jet
     * @param debug If debug job steps are required
     * @return The job if submitted
     * @throws Exception If the job is rejected as a duplicate is still running
     */
    public static Job submitCvaStpJob(HazelcastInstance hazelcastInstance, LocalDate calcDate,
            int batchSize, int parallelism, boolean debug, boolean useViridian) throws Exception {
        long timestamp = System.currentTimeMillis();
        String timestampStr = MyUtils.timestampToISO8601(timestamp);

        String jobNamePrefix = CvaStpJob.JOB_NAME_PREFIX;
        String jobName = jobNamePrefix + "$" + calcDate + "@" + timestampStr;
        String cppLoadBalancer = getLoadBalancer(hazelcastInstance);
        LOGGER.info("Using '{}' for C++ service", cppLoadBalancer);

        Pipeline pipeline = CvaStpJob.buildPipeline(jobName, timestamp, calcDate, cppLoadBalancer,
                batchSize, parallelism, debug, useViridian);

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(jobName);
        jobConfig.addClass(CounterpartyAggregator.class);
        jobConfig.addClass(CsvFileAsByteArray.class);
        jobConfig.addClass(CvaStpJob.class);
        jobConfig.addClass(CvaStpUtils.class);
        jobConfig.addClass(ExposureToCvaExposure.class);
        jobConfig.addClass(MtmToExposure.class);
        jobConfig.addClass(TradeExposureAggregator.class);
        jobConfig.addClass(XlstDataAsObjectArrayArray.class);
        jobConfig.addClass(XlstFileAsByteArray.class);

        Job job = MyUtils.findRunningJobsWithSamePrefix(jobNamePrefix, hazelcastInstance);
        if (job != null) {
            String message = String.format("Previous job '%s' id=='%d' still at status '%s'",
                    job.getName(), job.getId(), job.getStatus());
            throw new RuntimeException(message);
        } else {
            return hazelcastInstance.getJet().newJobIfAbsent(pipeline, jobConfig);
        }
    }


    /**
     * <p>Find the Load Balancer to use to access C++.
     * </p>
     *
     * @return Hostname.
     */
    private static String getLoadBalancer(HazelcastInstance hazelcastInstance) throws Exception {
        IMap<String, String> cvaConfigMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_CVA_CONFIG);

        String value = cvaConfigMap.get(MyConstants.CONFIG_CPP_SERVICE_KEY);

        if (value == null || value.isBlank()) {
            String message = "No config for " + MyConstants.CONFIG_CPP_SERVICE_KEY;
            throw new RuntimeException(message);
        }
        return value;
    }

}
