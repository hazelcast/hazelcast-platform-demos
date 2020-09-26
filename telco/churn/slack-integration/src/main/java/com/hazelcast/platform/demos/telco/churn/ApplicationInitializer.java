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

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>XXX
 * </p>
 */
@Configuration
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Autowired
    private JetInstance jetInstance;
    @Autowired
    private MyProperties myProperties;

    /**
     * <p>XXX
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            HazelcastInstance hazelcastInstance = this.jetInstance.getHazelcastInstance();
            LOGGER.info("-=-=-=-=- START {} START -=-=-=-=-=-", hazelcastInstance.getName());

            long timestamp = System.currentTimeMillis();
            String timestampStr = MyUtils.timestampToISO8601(timestamp);
            String jobNamePrefix = TopicToSlack.JOB_NAME_PREFIX;
            String jobName = jobNamePrefix + "@" + timestampStr;

            /* Job still works if Slack properties aren't present, but logs to STDOUT instead
             */
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
                }
            }

            Pipeline pipeline = TopicToSlack.buildPipeline(properties, MyConstants.ITOPIC_NAME_SLACK);

            JobConfig jobConfig = new JobConfig();
            jobConfig.setName(jobName);
            jobConfig.addClass(TopicToSlack.class);
            jobConfig.addClass(MyTopicSource.class);
            jobConfig.addClass(MySlackSink.class);
            jobConfig.addClass(JSONObject.class);

            Job job = MyUtils.findRunningJobsWithSamePrefix(jobNamePrefix, this.jetInstance);
            if (job != null) {
                String message = String.format("Previous job '%s' id=='%d' still at status '%s'",
                        job.getName(), job.getId(), job.getStatus());
                throw new RuntimeException(message);
            } else {
                job = jetInstance.newJobIfAbsent(pipeline, jobConfig);
                LOGGER.info("Submitted {}", job);
            }

            LOGGER.info("-=-=-=-=-  END  {}  END  -=-=-=-=-=-", hazelcastInstance.getName());
            hazelcastInstance.shutdown();
        };
    }

}
