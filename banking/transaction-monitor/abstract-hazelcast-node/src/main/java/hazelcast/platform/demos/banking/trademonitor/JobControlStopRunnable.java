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

package hazelcast.platform.demos.banking.trademonitor;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;

/**
 * <p>Stops at most one job with the given name.
 * </p>
 */
public class JobControlStopRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobControlStopRunnable.class);

    private final HazelcastInstance hazelcastInstance;
    private final String targetJobNamePrefix;

    public JobControlStopRunnable(String arg0) {
        this.hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
        this.targetJobNamePrefix = arg0;
    }

    @Override
    public void run() {
        List<Job> jobs = this.hazelcastInstance.getJet().getJobs();
        boolean found = false;

        for (Job job : jobs) {
            // Name may be null
            String jobName = Objects.toString(job.getName());
            String jobNamePrefix = jobName.split("@")[0];
            if (jobNamePrefix.equals(targetJobNamePrefix)) {
                JobStatus jobStatus = job.getStatus();
                if ((jobStatus == JobStatus.RUNNING)) {
                    found = true;
                    try {
                        job.cancel();
                        LOGGER.info("Cancelling '{}'", job);
                    } catch (Exception e) {
                        LOGGER.error("Failed issuing stop to " + job.getId(), e);
                    }
                } else {
                    LOGGER.debug("STOP job with prefix '{}', ignoring '{}' due to status", this.targetJobNamePrefix, job);
                }
            }
        }
        if (!found) {
            LOGGER.error("STOP job with prefix '{}', failed to find any matches");
        }
    }
}
