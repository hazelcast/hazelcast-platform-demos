/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.retail.clickstream.CsvField;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Launch RandomForest retraining or validation, depending on the command
 * </p>
 */
@Slf4j
public class RetrainingLaunchListenerRunnable implements Runnable {

    private static final int TEN = 10;
    private static final long THIRTY = 30L;

    private final HazelcastInstance hazelcastInstance;
    private final EntryEvent<Long, HazelcastJsonValue> entryEvent;

    public RetrainingLaunchListenerRunnable(EntryEvent<Long, HazelcastJsonValue> arg0) {
        this.hazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
        this.entryEvent = arg0;
    }

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "new JSONObject() *can* throw an exception")
    @Override
    public void run() {
        try {
            JSONObject jsonObject = new JSONObject(entryEvent.getValue().toString());
            String action = jsonObject.getString("action");
            long previous = jsonObject.getLong("previous");
            long count = jsonObject.getLong("count");
            long start = jsonObject.getLong("start");
            long end = jsonObject.getLong("end");
            long timestamp = jsonObject.getLong("timestamp");
            long keyTimestamp;
            if (MyConstants.RETRAINING_CONTROL_ACTION_VALIDATE.equals(action)) {
                keyTimestamp = previous;
            } else {
                keyTimestamp = timestamp;
            }

            Instant instant = Instant.ofEpochMilli(keyTimestamp);
            LocalDateTime when = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            String keyTimestampStr = DateTimeFormatter.ISO_DATE_TIME.format(when);
            String modelName = "RandomForest-" + keyTimestamp;

            String num = RetrainingLaunchListenerRunnable
                    .findNum(count, MyConstants.RETRAINING_CONTROL_ACTION_VALIDATE.equals(action));

            String jobNameRandomForestRetraining = RandomForestRetraining.class.getSimpleName()
                    + "@" + num + "@" + keyTimestamp + "@" + keyTimestampStr;
            String jobNameRandomForestValidation = RandomForestValidation.class.getSimpleName()
                    + "@" + num + "@" + keyTimestamp + "@" + keyTimestampStr;

            if (MyConstants.RETRAINING_CONTROL_ACTION_VALIDATE.equals(action)) {
                // Validation waits until training finishes AND produces output
                this.waitTilFinished(jobNameRandomForestRetraining, jobNameRandomForestValidation);
                IMap<String, String> modelVaultMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_MODEL_VAULT);
                if (modelVaultMap.containsKey(modelName)) {
                    Pipeline pipelineRandomForestValidation =
                            RandomForestValidation.buildPipeline(start, end, modelName);

                    JobConfig jobConfigRandomForestValidation = new JobConfig();
                    jobConfigRandomForestValidation.addClass(RandomForestValidation.class);
                    jobConfigRandomForestValidation.setName(jobNameRandomForestValidation);

                    this.validationGracePeriodWait(jobNameRandomForestValidation);

                    this.launchRecurringJob(pipelineRandomForestValidation, jobConfigRandomForestValidation);
                } else {
                    log.warn("run(): No stored model for key '{}', skipping validation."
                            + " Probably retraining run skipped as another still running.", modelName);
                }
            } else {
                // Training does not depend on previous validation
                Pipeline pipelineRandomForestRetraining = RandomForestRetraining.buildPipeline(start, end, modelName);

                JobConfig jobConfigRandomForestRetraining = new JobConfig();
                jobConfigRandomForestRetraining.addClass(RandomForestRetraining.class);
                jobConfigRandomForestRetraining.setName(jobNameRandomForestRetraining);

                this.launchRecurringJob(pipelineRandomForestRetraining, jobConfigRandomForestRetraining);
            }
        } catch (Exception e) {
            log.error("run()", e);
        }
    }

    /**
     * <p>Validation compares the prediction against the presence or absence
     * of an ordered. Wait a grace period to see if the order appears or not.
     * </p>
     *
     * @param jobNameRandomForestValidation
     */
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Exception in TimeUnit.sleep() only if interrupted")
    private void validationGracePeriodWait(String jobNameRandomForestValidation) {
        log.info("validationGracePeriodWait(): Wait {} seconds to submit '{}' so '{}' action has a chance to arrive.",
                MyConstants.VALIDATION_GRACE_PERIOD, jobNameRandomForestValidation, CsvField.ordered);
        try {
            TimeUnit.SECONDS.sleep(MyConstants.VALIDATION_GRACE_PERIOD);
        } catch (Exception ignored) {
        }
    }

    /**
     * <p>Wait at most <i>n</i> iterations to give a running job a chance
     * to complete
     * </p>
     *
     * @param jobNameToCheck
     * @param jobNameToSubmit
     */
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Exception in TimeUnit.sleep() only if interrupted")
    private void waitTilFinished(String jobNameToCheck, String jobNameToSubmit) {
        String jobNameToCheckBaseName = jobNameToCheck.split("@")[0];
        log.trace("waitTilFinished(): check for '{}' before submitting '{}'",
                jobNameToCheckBaseName, jobNameToSubmit);
        for (Job existingJob : this.hazelcastInstance.getJet().getJobs()) {
            if (existingJob.getName() != null) {
                String existingJobBaseName = existingJob.getName().split("@")[0];
                if (existingJobBaseName.equals(jobNameToCheckBaseName)) {
                    int count = 0;
                    while (count < TEN
                           && (existingJob.getStatus() == JobStatus.STARTING
                               || existingJob.getStatus() == JobStatus.RUNNING)) {
                        count++;
                        log.info("waitTilFinished(): Wait #{} to submit '{}' as '{}' has status {}",
                                count, jobNameToSubmit, existingJob.getName(), existingJob.getStatus());
                        try {
                            TimeUnit.SECONDS.sleep(THIRTY);
                        } catch (Exception ignored) {
                        }
                    }
                    if (count == TEN) {
                        log.warn("waitTilFinished(): Gave up waiting to submit '{}' as '{}' has status {}",
                                jobNameToSubmit, existingJob.getName(), existingJob.getStatus());
                    }
                }
            }
        }
    }

    /**
     * <p>Submit a job that may already be running.
     * </p>
     *
     * @param pipeline
     * @param jobConfig
     */
    private void launchRecurringJob(Pipeline pipeline, JobConfig jobConfig) {
        try {
            String newJobBaseName = jobConfig.getName().split("@")[0];
            log.trace("launchRecurringJob(): clickstream-green-hazelcast-0  -> check {}", newJobBaseName);
            for (Job existingJob : this.hazelcastInstance.getJet().getJobs()) {
                if (existingJob.getName() != null) {
                    String existingJobBaseName = existingJob.getName().split("@")[0];
                    if (existingJobBaseName.equals(newJobBaseName)) {
                        if (existingJob.getStatus() != JobStatus.COMPLETED
                                && existingJob.getStatus() != JobStatus.COMPLETING) {
                            // Only run if previous worked, not failed or still running
                            log.warn("launchRecurringJob(): Not submitting new job '{}' as '{}' has status {}",
                                    jobConfig.getName(), existingJob.getName(), existingJob.getStatus());
                            return;
                        }
                    }
                }
            }
            // Fails if job exists with same job name, unlike "newJobIfAbsent"
            log.trace("launchRecurringJob(): submit '{}'", jobConfig.getName());
            Job job = this.hazelcastInstance.getJet().newJob(pipeline, jobConfig);
            log.info("launchRecurringJob(): '{}' -> submitted, Id: {}",
                    jobConfig.getName(), job.getId());
        } catch (Exception e) {
            log.error("launchRecurringJob() for " + jobConfig, e);
        }
    }

    /**
     * <p>Deduce the job sequence number from the input count.
     * </p>
     *
     * @param count Input count
     * @param isValidate Are we doing validation or training
     * @return
     */
    public static String findNum(long count, boolean isValidate) {
        int countActionPoints = 0;
        long l = count;
        while (l > 0) {
            if (l >= MyConstants.RETRAINING_INTERVAL) {
                countActionPoints++;
            }
            l = l - MyConstants.RETRAINING_INTERVAL;
        }
        int num = 0;
        if (isValidate) {
            num = countActionPoints / 2;
        } else {
            num = (1 + countActionPoints) / 2;
        }
        return String.valueOf(num);
    }
}
