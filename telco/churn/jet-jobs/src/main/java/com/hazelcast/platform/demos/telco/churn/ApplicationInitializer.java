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

package com.hazelcast.platform.demos.telco.churn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>A job launcher for mandatory processing jobs
 * </p>
 * <ol>
 * <li>
 * <p>{@link KafkaIngest}</p>
 * <p>XXX
 * </p>
 * </li>
 * <li>
 * <p>{@link CassandraDebeziumCDC}</p>
 * <p>XXX
 * </p>
 * </li>
 * </ol>
 */
@Configuration
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Autowired
    private JetInstance jetInstance;
    @Autowired
    private MyProperties myProperties;

    /**
     * <p>Launch for mandatory input process, uploading call data records from
     * Kafka, and amended call data records from Cassandra if they are corrected
     * after upload.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            HazelcastInstance hazelcastInstance = this.jetInstance.getHazelcastInstance();
            LOGGER.info("-=-=-=-=- START {} START -=-=-=-=-=-", hazelcastInstance.getName());

            long timestamp = System.currentTimeMillis();
            String timestampStr = MyUtils.timestampToISO8601(timestamp);

            JobConfig jobConfigKafkaIngest = KafkaIngest.buildJobConfig(timestampStr);
            Pipeline pipelineKafkaIngest =
                    KafkaIngest.buildPipeline(this.myProperties.getBootstrapServers());

            JobConfig jobConfigCassandraDebeziumCDC = CassandraDebeziumCDC.buildJobConfig(timestampStr);
            Pipeline pipelineCassandraDebeziumCDC =
                    CassandraDebeziumCDC.buildPipeline();

            this.trySubmit(jobConfigKafkaIngest, pipelineKafkaIngest);
            this.trySubmit(jobConfigCassandraDebeziumCDC, pipelineCassandraDebeziumCDC);

            LOGGER.info("-=-=-=-=-  END  {}  END  -=-=-=-=-=-", hazelcastInstance.getName());
            hazelcastInstance.shutdown();
        };
    }

    /**
     * <p>Jobs are named "{@code something@timestamp}" and we only wish one of each running.
     * Check for running jobs with the same prefix before attempting to submit. This
     * mechanism isn't rock solid, as between checking and submitting another process
     * could submit. However (a) this is a demo, and (b) it wouldn't matter too much
     * here to produce double-output, as processing is idempotent, it's more for
     * elegance. For a more robust solution, use Hazelcast's
     * {@link com.hazelcast.cp.lock.FencedLock FencedLock}.
     * </p>
     *
     * @param jobConfig
     * @param pipeline
     * @throws Exception
     */
    private void trySubmit(JobConfig jobConfig, Pipeline pipeline) throws Exception {
        String jobName = jobConfig.getName();
        int atSymbol = jobName.indexOf('@');
        if (atSymbol < 0) {
            LOGGER.error("Not submitting '{}', name missing '@' separator", jobName);
            return;
        }
        String jobNamePrefix = jobName.substring(atSymbol);

        if (pipeline == null) {
            LOGGER.error("Not submitting '{}', pipeline is null", jobName);
            return;
        }

        Job job = MyUtils.findRunningJobsWithSamePrefix(jobNamePrefix, this.jetInstance);
        if (job != null) {
            String message = String.format("Previous job '%s' id=='%d' still at status '%s'",
                    job.getName(), job.getId(), job.getStatus());
            throw new RuntimeException(message);
        } else {
            job = jetInstance.newJobIfAbsent(pipeline, jobConfig);
            LOGGER.info("Submitted {}", job);
        }
    }

}