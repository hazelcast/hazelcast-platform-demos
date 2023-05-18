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

package com.hazelcast.platform.demos.banking.cva;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.platform.demos.banking.cva.ws.MySocketJobListener;
import com.hazelcast.topic.ITopic;

/**
 * <p>The main "{@code run()}" method of the application, called
 * once configuration created.
 * </p>
 */
@Component
public class ApplicationRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final long FIVE = 5L;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MySocketJobListener mySocketJobListener;

    /**
     * <p>Polls for changes to job state, and publishes to a topic to feed to a web socket.
     * </p>
     * <p>See also <a href="https://github.com/hazelcast/hazelcast-jet/issues/2206">Issue 2206</a>
     * </p>
     *
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {

        ITopic<Tuple2<HazelcastJsonValue, JobStatus>> jobStateTopic =
                this.hazelcastInstance.getTopic(MyConstants.ITOPIC_NAME_JOB_STATE);

        jobStateTopic.addMessageListener(this.mySocketJobListener);

        Map<Long, JobStatus> currentState;
        Map<Long, JobStatus> previousState = new TreeMap<>(Collections.reverseOrder());
        Tuple2<HazelcastJsonValue, JobStatus> dummyMessage = Tuple2.tuple2(this.jobToJson(null), JobStatus.NOT_RUNNING);

        while (true) {
            try {
                // Checkstyle thinks the below is more obvious than "TimeUnit.SECONDS.sleep(5)"
                TimeUnit.SECONDS.sleep(FIVE);
                // Nudge page to refresh even when nothing happening
                jobStateTopic.publish(dummyMessage);

                currentState = this.hazelcastInstance.getJet().getJobs()
                        .stream()
                        .collect(Collectors.toMap(Job::getId, Job::getStatus));

                // Live jobs, may be new or existing
                for (Entry<Long, JobStatus> entry : currentState.entrySet()) {
                    JobStatus oldJobStatus = previousState.get(entry.getKey());
                    JobStatus newJobStatus = entry.getValue();

                    Job job = null;
                    try {
                        job = this.hazelcastInstance.getJet().getJob(entry.getKey());
                    } catch (Exception e) {
                        LOGGER.error("Live:," + entry.getKey().toString(), e);
                    }

                    if (job != null) {
                        HazelcastJsonValue json = this.jobToJson(job);

                        Tuple2<HazelcastJsonValue, JobStatus> message = Tuple2.tuple2(json, oldJobStatus);

                        // Only log delta but publish baseline
                        if (oldJobStatus == null || oldJobStatus != newJobStatus) {
                            LOGGER.debug("Job state change: '{}'", message);
                        }
                        jobStateTopic.publish(message);

                        // Remove from previous state once examined
                        previousState.remove(entry.getKey());
                    }
                }

                // Dead jobs
                for (Entry<Long, JobStatus> entry : previousState.entrySet()) {
                    Job job = null;
                    try {
                        job = this.hazelcastInstance.getJet().getJob(entry.getKey());
                    } catch (Exception e) {
                        LOGGER.error("Dead:," + entry.getKey().toString(), e);
                    }

                    if (job != null) {
                        HazelcastJsonValue json = this.jobToJson(job);
                        Tuple2<HazelcastJsonValue, JobStatus> message = Tuple2.tuple2(json, entry.getValue());
                        jobStateTopic.publish(message);
                    }
                }

                previousState = currentState;

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * <p>Makes a JSON representation of a Job, for HTML display.
     * Only select some fields.
     * </p>
     *
     * @param job
     * @return
     */
    private HazelcastJsonValue jobToJson(Job job) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{ ");

        stringBuilder.append(" \"dummy\": " + (job == null));

        if (job != null) {
            stringBuilder.append(", \"id\": \"" + job.getId() + "\"");
            stringBuilder.append(", \"name\": \"" + (job.getName() == null ? "" : job.getName()) + "\"");
            stringBuilder.append(", \"status\": \"" + job.getStatus() + "\"");
            stringBuilder.append(", \"submission_time\": \"" + job.getSubmissionTime() + "\"");
        } else {
            stringBuilder.append(", \"id\": \"\"");
            stringBuilder.append(", \"name\": \"\"");
            stringBuilder.append(", \"status\": \"\"");
            stringBuilder.append(", \"submission_time\": \"\"");
        }

        stringBuilder.append(" }");

        return new HazelcastJsonValue(stringBuilder.toString());
    }

}
