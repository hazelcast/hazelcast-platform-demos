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

package com.hazelcast.platform.demos.banking.cva;

import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * <p>Required properties for the application, with the
 * prefix of "{@code my}".
 * </p>
 * <p>Spring will set the field "{@code site}" if there is
 * a property "{@code site}" in the environment or in one
 * of the application properties fields loaded.
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
    private String project;
    @NotNull
    private Site site;
    @NotNull
    private Site remoteSite;
    @NotNull
    private int partitions;
    @NotNull
    private int initSize;
    @NotNull
    private boolean useViridian;
    @NotNull
    private String viridianCluster1DiscoveryToken;
    @NotNull
    private String viridianCluster1Id;
    @NotNull
    private String viridianCluster1KeyPassword;

    public String getBuildTimestamp() {
        return this.buildTimestamp;
    }
    public void setBuildTimestamp(String buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
        LOGGER.info("myProperties.buildTimestamp=='{}'", this.buildTimestamp);
    }
    public int getInitSize() {
        return this.initSize;
    }
    public void setInitSize(int initSize) {
        this.initSize = initSize;
        LOGGER.info("myProperties.initSize=='{}'", this.initSize);
    }
    public int getPartitions() {
        return this.partitions;
    }
    public void setPartitions(int partitions) {
        this.partitions = partitions;
        LOGGER.info("myProperties.partitions=='{}'", this.partitions);
    }
    public String getProject() {
        return this.project;
    }
    public void setProject(String project) {
        this.project = project;
        LOGGER.info("myProperties.project=='{}'", this.project);
    }
    public Site getRemoteSite() {
        return this.remoteSite;
    }
    public Site getSite() {
        return this.site;
    }
    public void setSite(Site site) {
        this.site = site;
        if (this.site == Site.SITE1) {
            this.remoteSite = Site.SITE2;
        } else {
            this.remoteSite = Site.SITE1;
        }
        LOGGER.info("myProperties.site=='{}'", this.site);
        LOGGER.info("myProperties.remoteSite=='{}'", this.remoteSite);
    }
    public boolean isUseViridian() {
        return useViridian;
    }
    public void setUseViridian(boolean useViridian) {
        this.useViridian = useViridian;
    }
    public String getViridianCluster1DiscoveryToken() {
        return viridianCluster1DiscoveryToken;
    }
    public void setViridianCluster1DiscoveryToken(String viridianCluster1DiscoveryToken) {
        this.viridianCluster1DiscoveryToken = viridianCluster1DiscoveryToken;
    }
    public String getViridianCluster1Id() {
        return viridianCluster1Id;
    }
    public void setViridianCluster1Id(String viridianCluster1Id) {
        this.viridianCluster1Id = viridianCluster1Id;
    }
    public String getViridianCluster1KeyPassword() {
        return viridianCluster1KeyPassword;
    }
    public void setViridianCluster1KeyPassword(String viridianCluster1KeyPassword) {
        this.viridianCluster1KeyPassword = viridianCluster1KeyPassword;
    }

}
