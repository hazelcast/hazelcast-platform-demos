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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.logging.ILogger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Launched by {@link AlertLoggerManager}, suspend/resume another job, {@link AlertLogger}
 * </p>
 */
public class AlertLoggerManagerRunnable implements Runnable {

    private static final Logger LOGGER_TO_IMAP =
            IMapLoggerFactory.getLogger(AlertLoggerManagerRunnable.class);

    private final HazelcastInstance hazelcastInstance;
    private final ILogger iLogger;
    private final Object object;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Need HazelcastInstance for job control")
    public AlertLoggerManagerRunnable(HazelcastInstance arg0, Object arg1) {
        this.hazelcastInstance = arg0;
        this.iLogger = this.hazelcastInstance.getLoggingService().getLogger(AlertLoggerManagerRunnable.class);
        this.object = arg1;
    }

    /**
     * <p>Job may not be running, eg. if errors.
     * </p>
     */
    @Override
    public void run() {
        try {
            JSONObject jsonObject = new JSONObject(this.object.toString());

            String jobName = jsonObject.getString(MyConstants.MONGO_COLLECTION_FIELD1);
            String requiredState = jsonObject.getString(MyConstants.MONGO_COLLECTION_FIELD2);

            boolean found = false;
            for (Job job : this.hazelcastInstance.getJet().getJobs()) {
                if (jobName.equals(job.getName())) {
                    found = true;

                    if (requiredState.equals(job.getStatus().toString())) {
                        if (this.iLogger.isWarningEnabled()) {
                            String message = String.format("Job '%s' already has required state '%s'",
                                    jobName, requiredState);
                            this.iLogger.warning(message);
                        }
                    } else {
                        this.amendState(job, requiredState);
                    }
                }
            }

            /* Can't suspend if not running.
             * Can make running if not running.
             */
            if (!found) {
                if (requiredState.equals(JobStatus.RUNNING.toString())) {
                    if (this.iLogger.isInfoEnabled()) {
                        String message = String.format("Job '%s' not found, starting as required state '%s'",
                                jobName, requiredState);
                        this.iLogger.info(message);
                    }

                    Pipeline pipeline = AlertLogger.buildPipeline();

                    JobConfig jobConfig = new JobConfig();
                    jobConfig.setName(jobName);
                    jobConfig.addClass(AlertLogger.class);

                    Job job = this.hazelcastInstance.getJet().newJobIfAbsent(pipeline, jobConfig);
                    LOGGER_TO_IMAP.info(Objects.toString(job));
                } else {
                    if (this.iLogger.isInfoEnabled()) {
                        String message = String.format("Job '%s' not found, ignoring as required state '%s'",
                                jobName, requiredState);
                        this.iLogger.info(message);
                    }
                }
            }
        } catch (Exception e) {
            if (this.iLogger.isSevereEnabled()) {
                String message = String.format("run(): %s", e.getMessage());
                this.iLogger.severe(message);
            }
        }
    }

    /**
     * <p>Change job state, suspend/resume.
     * </p>
     *
     * @param job
     * @param requiredState
     */
    private void amendState(Job job, String requiredState) throws Exception {
        if (requiredState.equals(JobStatus.RUNNING.toString())) {
            job.resume();
        } else {
            if (requiredState.equals(JobStatus.SUSPENDED.toString())) {
                job.suspend();
            } else {
                if (this.iLogger.isSevereEnabled()) {
                    String message = String.format("run():amendState(): '%s' unhandled state '%s'",
                            job.getName(), job.getStatus().toString());
                    this.iLogger.severe(message);
                }
            }
        }

        /* Confirm if action happened
         */
        if (!this.iLogger.isInfoEnabled()) {
            return;
        } else {
            String message = String.format("run():amendState(): wait 1 minute to confirm new state of '%s'",
                    job.getName());
            this.iLogger.info(message);
            TimeUnit.MINUTES.sleep(1L);
            if (requiredState.equals(job.getStatus().toString())) {
                message = String.format("run():amendState(): '%s' state change success '%s'",
                        job.getName(), job.getStatus().toString());
                this.iLogger.info(message);
                LOGGER_TO_IMAP.info(message);
            } else {
                message = String.format("run():amendState(): '%s' state change failure '%s'",
                        job.getName(), job.getStatus().toString());
                this.iLogger.severe(message);
                LOGGER_TO_IMAP.info(message);
            }
        }
    }

}
