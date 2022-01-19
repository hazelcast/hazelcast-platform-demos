/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>A job launcher for the optional Slack jobs
 * </p>
 * <ol>
 * <li>
 * <p>{@link TopicToSlack}</p>
 * <p>Always submitted, re-publishes from a Hazelcast {@link com.hazelcast.topic.ITopic}
 * to Slack if Slack credentials exist, or to the STDOUT otherwise.
 * </p>
 * </li>
 * <li>
 * <p>{@link SlackToSlackCLI}</p>
 * <p>Possibly submitted, reads from Slack and attempts to use input as a command, hence
 * only valid to try to run if Slack credentials exist.
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

    /**
     * <p>Launch jobs that connect to Slack, if Slack connectivity credentials
     * have been provided.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            LOGGER.info("-=-=-=-=- START '{}' START -=-=-=-=-=-", hazelcastInstance.getName());

            long timestamp = System.currentTimeMillis();
            String timestampStr = MyUtils.timestampToISO8601(timestamp);

            String jobNamePrefixTopicToSlack = TopicToSlack.JOB_NAME_PREFIX;
            String jobNameTopicToSlack = jobNamePrefixTopicToSlack + "@" + timestampStr;

            String jobNamePrefixSlackToSlackCLI = SlackToSlackCLI.JOB_NAME_PREFIX;
            String jobNameSlackToSlackCLI = jobNamePrefixSlackToSlackCLI + "@" + timestampStr;

            /* Slack publish still works if Slack properties aren't present, but publishess to STDOUT instead.
             * Slack reader obviously can't.
             */
            boolean slackUseable = false;
            Properties properties = new Properties();
            if (this.myProperties.getSlackAccessToken() != null
                    && this.myProperties.getSlackChannelId() != null
                    && this.myProperties.getSlackChannelName() != null) {
                if (this.myProperties.getSlackAccessToken().length() > 0
                        && this.myProperties.getSlackChannelId().length() > 0
                        && this.myProperties.getSlackChannelName().length() > 0) {
                    properties.setProperty(MyConstants.SLACK_ACCESS_TOKEN, this.myProperties.getSlackAccessToken());
                    properties.setProperty(MyConstants.SLACK_CHANNEL_ID, this.myProperties.getSlackChannelId());
                    properties.setProperty(MyConstants.SLACK_CHANNEL_NAME, this.myProperties.getSlackChannelName());
                    slackUseable = true;
                }
            }

            String projectName = this.myProperties.getProject();
            Pipeline pipelineTopicToSlack =
                    TopicToSlack.buildPipeline(properties, MyConstants.ITOPIC_NAME_SLACK, projectName);

            JobConfig jobConfigTopicToSlack = new JobConfig();
            jobConfigTopicToSlack.setName(jobNameTopicToSlack);
            jobConfigTopicToSlack.addClass(TopicToSlack.class);
            jobConfigTopicToSlack.addClass(MyTopicSource.class);
            jobConfigTopicToSlack.addClass(MySlackSink.class);

            Pipeline pipelineSlackToSlackCLI = SlackToSlackCLI.buildPipeline(properties, projectName);

            JobConfig jobConfigSlackToSlackCLI = new JobConfig();
            jobConfigSlackToSlackCLI.setName(jobNameSlackToSlackCLI);
            jobConfigSlackToSlackCLI.addClass(SlackToSlackCLI.class);
            jobConfigSlackToSlackCLI.addClass(MySlackSource.class);
            jobConfigSlackToSlackCLI.addClass(MySlackSink.class);
            jobConfigSlackToSlackCLI.addClass(MyUtils.class);

            this.trySubmit(jobNamePrefixTopicToSlack, jobConfigTopicToSlack, pipelineTopicToSlack);
            if (slackUseable) {
                this.trySubmit(jobNamePrefixSlackToSlackCLI, jobConfigSlackToSlackCLI, pipelineSlackToSlackCLI);
            }

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
     * @param jobNamePrefix
     * @param jobConfig
     * @param pipeline
     * @throws Exception
     */
    private void trySubmit(String jobNamePrefix, JobConfig jobConfig, Pipeline pipeline) throws Exception {
        Job job = MyUtils.findRunningJobsWithSamePrefix(jobNamePrefix, this.hazelcastInstance);
        if (job != null) {
            String message = String.format("Previous job '%s' id=='%d' still at status '%s'",
                    job.getName(), job.getId(), job.getStatus());
            throw new RuntimeException(message);
        } else {
            job = this.hazelcastInstance.getJet().newJobIfAbsent(pipeline, jobConfig);
            LOGGER.info("Submitted {}", job);
        }
    }

}
