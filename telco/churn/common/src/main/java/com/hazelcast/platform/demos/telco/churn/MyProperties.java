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

package com.hazelcast.platform.demos.telco.churn;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * <p>Required properties for the application, with the
 * prefix of "{@code my}".
 * </p>
 * <p>Fields in this class are marked as "{@code @NotNull}" so will fail
 * fast if omitted, and are logged by their setters.
 * </p>
 */
@ConfigurationProperties("my")
@Validated
public class MyProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyProperties.class);

    @NotNull
    private String buildTimestamp;
    @NotNull
    private String buildUserName;
    // Can't be null
    private int initSize;
    @NotNull
    private String project;
    // Null allowed for following
    private String bootstrapServers;
    private String site;
    private String slackAccessToken;
    private String slackChannelId;
    private String slackChannelName;

    public String getBootstrapServers() {
        return this.bootstrapServers;
    }
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        LOGGER.info("myProperties.bootstrapServers=='{}'", this.bootstrapServers);
    }

    public String getBuildTimestamp() {
        return this.buildTimestamp;
    }
    public void setBuildTimestamp(String buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
        LOGGER.info("myProperties.buildTimestamp=='{}'", this.buildTimestamp);
    }

    public String getBuildUserName() {
        return this.buildUserName;
    }
    public void setBuildUserName(String buildUserName) {
        this.buildUserName = buildUserName;
        LOGGER.info("myProperties.buildUserName=='{}'", this.buildUserName);
    }

    public int getInitSize() {
        return this.initSize;
    }
    public void setInitSize(int initSize) {
        this.initSize = initSize;
        LOGGER.info("myProperties.initSize=='{}'", this.initSize);
    }

    public String getProject() {
        return this.project;
    }
    public void setProject(String project) {
        this.project = project;
        LOGGER.info("myProperties.project=='{}'", this.project);
    }

    public String getSite() {
        return this.site;
    }
    public void setSite(String site) {
        this.site = site;
        LOGGER.info("myProperties.site=='{}'", this.site);
    }

    public String getSlackAccessToken() {
        return this.slackAccessToken;
    }
    public void setSlackAccessToken(String slackAccessToken) {
        this.slackAccessToken = slackAccessToken;
        // Don't log details, only enough to confirm if set
        LOGGER.info("myProperties.slackAccessToken.length()=={}", this.slackAccessToken.length());
    }

    public String getSlackChannelId() {
        return this.slackChannelId;
    }
    public void setSlackChannelId(String slackChannelId) {
        this.slackChannelId = slackChannelId;
        // Don't log details, only enough to confirm if set
        LOGGER.info("myProperties.slackChannelId.length()=={}", this.slackChannelId.length());
    }

    public String getSlackChannelName() {
        return this.slackChannelName;
    }
    public void setSlackChannelName(String slackChannelName) {
        this.slackChannelName = slackChannelName;
        LOGGER.info("myProperties.slackChannelName=='{}'", this.slackChannelName);
    }

}
