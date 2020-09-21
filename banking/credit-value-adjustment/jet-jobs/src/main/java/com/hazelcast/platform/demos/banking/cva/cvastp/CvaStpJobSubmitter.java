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

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.platform.demos.banking.cva.MyConstants;
import com.hazelcast.platform.demos.banking.cva.MyUtils;

/**
 * <p>Submit the CVA job if the previous run has finished.
 * </p>
 */
public class CvaStpJobSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CvaStpJobSubmitter.class);

    private static final String CPP_DOCKER = "cva-cpp";
    private static final String CPP_KUBERNETES = "cpp-service";
    private static final String CPP_LOCALHOST = "127.0.0.1";
    private static final int PORT = 50001;

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
     * @param jetInstance Used to find similar named jobs
     * @param calcDate Calculation date to use
     * @return The job if submitted
     * @throws Exception If the job is rejected as a duplicate is still running
     */
    public static Job submitCvaStpJob(JetInstance jetInstance, LocalDate calcDate) throws Exception {
        boolean debug = false;
        int batchSize = MyConstants.DEFAULT_BATCH_SIZE;
        int parallelism = 1;
        return CvaStpJobSubmitter.submitCvaStpJob(jetInstance, calcDate, batchSize, parallelism, debug);
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
     * @param jetInstance Used to find similar named jobs
     * @param calcDate Calculation date to use
     * @param batchSize How many calcs to pass to C++
     * @param parallelism How many C++ workers to each each Jet
     * @param debug If debug job steps are required
     * @return The job if submitted
     * @throws Exception If the job is rejected as a duplicate is still running
     */
    public static Job submitCvaStpJob(JetInstance jetInstance, LocalDate calcDate,
            int batchSize, int parallelism, boolean debug) throws Exception {
        long timestamp = System.currentTimeMillis();
        String timestampStr = MyUtils.timestampToISO8601(timestamp);

        String jobNamePrefix = CvaStpJob.JOB_NAME_PREFIX;
        String jobName = jobNamePrefix + "$" + calcDate + "@" + timestampStr;
        String cppLoadBalancer = getLoadBalancer();

        int maxParallelism = 2;
        if (parallelism > maxParallelism) {
            LOGGER.error("Experimental build, parallelism {} constrained to max parallelism {}",
                    parallelism, maxParallelism);
            parallelism = maxParallelism;
        }

        Pipeline pipeline = CvaStpJob.buildPipeline(jobName, timestamp, calcDate, cppLoadBalancer,
                PORT, batchSize, parallelism, debug);

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(jobName);
        jobConfig.addClass(CvaStpJob.class);

        Job job = MyUtils.findRunningJobsWithSamePrefix(jobNamePrefix, jetInstance);
        if (job != null) {
            String message = String.format("Previous job '%s' id=='%d' still at status '%s'",
                    job.getName(), job.getId(), job.getStatus());
            throw new RuntimeException(message);
        } else {
            return jetInstance.newJobIfAbsent(pipeline, jobConfig);
        }
    }


    /**
     * <p>Find the Load Balancer to use to access C++, based on
     * system properties and derivation.
     * </p>
     *
     * @return Hostname, no port.
     */
    private static String getLoadBalancer() {
        String cppService = System.getProperty("my.cpp.service", "");
        boolean dockerEnabled =
                System.getProperty("my.docker.enabled", "false").equalsIgnoreCase(Boolean.TRUE.toString());
        boolean kubernetesEnabled =
                System.getProperty("my.kubernetes.enabled", "false").equalsIgnoreCase(Boolean.TRUE.toString());

        //XXX Experiment build
        if (kubernetesEnabled || dockerEnabled) {
            cppService = CPP_LOCALHOST;
            LOGGER.error("Experimental build, use '{}'", cppService);
            return cppService;
        }

        // If set, validate but don't reject
        if (cppService.length() > 0) {
            validate(dockerEnabled, kubernetesEnabled, cppService);
        } else {
            // Unset, so guess
            //XXXif (!dockerEnabled && !kubernetesEnabled) {
                cppService = CPP_LOCALHOST;
            //}
            //XXXif (dockerEnabled && !kubernetesEnabled) {
            //    cppService = CPP_DOCKER;
            //}
            //XXXif (kubernetesEnabled) {
            //    cppService = CPP_KUBERNETES;
            //}
        }

        return cppService;
    }


    /**
     * <p>Validate how the C++ service is set, compared to localhost,
     * Docker or Kubernetes running.
     * </p>
     *
     * @param dockerEnabled False means localhost
     * @param kubernetesEnabled False means Docker or localhost
     * @param cppService The service URL
     */
    private static void validate(boolean dockerEnabled, boolean kubernetesEnabled, String cppService) {
        if (!dockerEnabled && !kubernetesEnabled && !cppService.equals(CPP_LOCALHOST)) {
            LOGGER.warn("localhost, but 'my.cpp.service'=='{}'", cppService);
        }
        if (dockerEnabled && !kubernetesEnabled && !cppService.equals(CPP_DOCKER)) {
            LOGGER.warn("Docker, but 'my.cpp.service'=='{}'", cppService);
        }
        if (kubernetesEnabled && !cppService.startsWith(CPP_KUBERNETES)) {
            LOGGER.warn("Kubernetes, but 'my.cpp.service'=='{}'", cppService);
        }
    }

}
