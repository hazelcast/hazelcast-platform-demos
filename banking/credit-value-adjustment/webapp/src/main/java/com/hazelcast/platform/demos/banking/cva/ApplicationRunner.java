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

package com.hazelcast.platform.demos.banking.cva;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.topic.ITopic;

/**
 * <p>The main "{@code run()}" method of the application, called
 * once configuration created.
 * </p>
 */
@Component
public class ApplicationRunner implements CommandLineRunner {

    private static final long FIVE = 5L;

    @Autowired
    private JetInstance jetInstance;

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

        ITopic<Tuple2<Job, JobStatus>> jobStateTopic =
                this.jetInstance.getHazelcastInstance().getTopic(MyConstants.ITOPIC_NAME_JOB_STATE);

        Map<Long, JobStatus> currentState;
        Map<Long, JobStatus> previousState = new HashMap<>();

        while (true) {
            try {
                // Checkstyle thinks the below is more obvious than "TimeUnit.SECONDS.sleep(5)"
                TimeUnit.SECONDS.sleep(FIVE);

                currentState = this.jetInstance.getJobs()
                        .stream()
                        .collect(Collectors.toMap(Job::getId, Job::getStatus));

                // Live jobs, may be new or existing
                for (Entry<Long, JobStatus> entry : currentState.entrySet()) {
                    JobStatus oldJobStatus = previousState.get(entry.getKey());
                    JobStatus newJobStatus = entry.getValue();

                    if (oldJobStatus == null || oldJobStatus != newJobStatus) {
                        Tuple2<Job, JobStatus> message = Tuple2.tuple2(this.jetInstance.getJob(entry.getKey()), oldJobStatus);
                        jobStateTopic.publish(message);
                    }

                    // Remove from previous state once examined
                    previousState.remove(entry.getKey());
                }

                // Dead jobs
                for (Entry<Long, JobStatus> entry : previousState.entrySet()) {
                    Tuple2<Job, JobStatus> message = Tuple2.tuple2(this.jetInstance.getJob(entry.getKey()), entry.getValue());
                    jobStateTopic.publish(message);
                }

                previousState = currentState;

            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
