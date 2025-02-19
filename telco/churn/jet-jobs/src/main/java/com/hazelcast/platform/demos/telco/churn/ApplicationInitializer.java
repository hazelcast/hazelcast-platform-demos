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

package com.hazelcast.platform.demos.telco.churn;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>A job launcher for mandatory processing jobs
 * </p>
 * <ol>
 * <li>
 * <p>{@link CassandraDebeziumTwoWayCDC}</p>
 * <p>Load changes from Cassandra of <u>historic</u> call
 * data records into Hazelcast. This data would change if
 * we made corrections to call data records for some
 * reason, such as the mast ID if mast IDs are updated.
 * </p>
 * </li>
 * <li>
 * <p>{@link KafkaIngest}</p>
 * <p>Read call data records from Kafka direct into Hazelcast.
 * These are the <u>new</u> calls, happening live, not the
 * historical calls stored in Cassandra. However they need
 * stored in Cassandra to become historic calls.
 * </p>
 * </li>
 * <li>
 * <p>{@link MongoDebeziumTwoWayCDC}</p>
 * <p>Load changes from Mongo made to customers into
 * Hazelcast. Other applications may be updating Mongo,
 * and we get these changes on a Kafka topic.
 * </p>
 * </li>
 * <li>
 * <p>{@link MySqlDebeziumOneWayCDC}</p>
 * <p>Load changes from MySql into Hazelcast. Hazelcast does not
 * write back to MySql (unlike Cassandra which it does), so do
 * not have to catch looping data.
 * </p>
 * </li>
 * <li>
 * <p>{@link MLChurnDetector}</p>
 * <p>Use "<i>Machine Learning inference</i>" to detect when
 * a customer is likely to churn. Runs a Python machine learning
 * model, which is pre-trained.
 * </p>
 * </li>
 * <li>
 * <p>{@link CassandraDebeziumTwoWayCDC}</p>
 * <p>Load changes from Cassandra into Hazelcast. Hazelcast will
 * write updates made in Hazelcast to Cassandra, so we have to
 * cope with CDC records that we originated.
 * </p>
 * </li>
 * </ol>
 */
@Configuration
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyProperties myProperties;
    @Value("${spring.datasource.username}")
    private String mySqlUsername;
    @Value("${spring.datasource.password}")
    private String mySqlPassword;

    /**
     * <p>Launch for mandatory input process, uploading call data records from
     * Kafka, and amended call data records from Cassandra if they are corrected
     * after upload.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            LOGGER.info("-=-=-=-=- START '{}' START -=-=-=-=-=-", this.hazelcastInstance.getName());

            var timestamp = System.currentTimeMillis();
            var bootstrapServers = this.myProperties.getBootstrapServers();
            LOGGER.debug("Kafka brokers: {}", bootstrapServers);

            List.of(
                    new CassandraDebeziumTwoWayCDC(timestamp, bootstrapServers),
                    new KafkaIngest(timestamp, bootstrapServers),
                    new MongoDebeziumTwoWayCDC(timestamp, bootstrapServers),
                    new MLChurnDetector(timestamp),
                    new MySqlDebeziumOneWayCDC(timestamp,
                            this.mySqlUsername, this.mySqlPassword)
                    )
            .stream()
            .filter(myJobWrapper -> {
                // Turn off any that are unwanted
                String jobName = myJobWrapper.getJobConfig().getName();
                boolean include = !jobName.contains("FILTER OUT NAME");
                if (!include) {
                    LOGGER.warn("Skip '{}', not wanted", jobName);
                }
                return include;
            })
            .forEach(this::trySubmit);

            LOGGER.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-", hazelcastInstance.getName());
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
     * @param myJobWrapper {@link JobConfig} and {@link Pipeline} together.
     */
    private void trySubmit(MyJobWrapper myJobWrapper) {
        JobConfig jobConfig = myJobWrapper.getJobConfig();
        Pipeline pipeline = myJobWrapper.getPipeline();

        String jobName = jobConfig.getName();
        int atSymbol = jobName.indexOf('@');
        if (atSymbol < 0) {
            LOGGER.error("Not submitting '{}', name missing '@' separator", jobName);
            return;
        }
        String jobNamePrefix = jobName.substring(atSymbol);

        if (pipeline == null) {
            throw new RuntimeException("Null pipeline for " + jobName);
        }

        Job job = MyUtils.findRunningJobsWithSamePrefix(jobNamePrefix, this.hazelcastInstance);
        if (job != null) {
            String message = String.format("Previous job '%s' id=='%d' still at status '%s'",
                    job.getName(), job.getId(), job.getStatus());
            throw new RuntimeException(message);
        } else {
            try {
                job = this.hazelcastInstance.getJet().newJobIfAbsent(pipeline, jobConfig);
                LOGGER.info("Submitted {}", job);
            } catch (Exception e) {
                // Add some helpful context, which job!
                throw new RuntimeException(jobName, e);
            }
        }
    }

}
