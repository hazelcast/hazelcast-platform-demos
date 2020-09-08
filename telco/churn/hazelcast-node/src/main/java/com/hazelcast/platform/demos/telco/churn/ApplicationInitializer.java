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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.jet.JetInstance;

/**
 * <p>
 * Ensure the server is in a ready state, by requesting all the set-up
 * processing runs. This is idempotent. All servers will request but only the
 * first to start will result in anything happening.
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
     * <p>
     * Use a Spring "{@code @Bean}" to kick off the necessary initialisation after
     * the objects we need are ready.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            boolean isLocalhost = !System.getProperty("my.docker.enabled", "false")
                    .equalsIgnoreCase(Boolean.TRUE.toString())
                    && !System.getProperty("my.kubernetes.enabled", "false").equalsIgnoreCase(Boolean.TRUE.toString());

            int currentSize = this.jetInstance.getCluster().getMembers().size();
            if (this.myProperties.getInitSize() > currentSize) {
                LOGGER.info("Cluster size {}, not initializing until {}", currentSize, this.myProperties.getInitSize());
            } else {
                this.createNeededObjects();
                this.launchNeededJobs(isLocalhost);
            }
        };
    }

    /**
     * <p>
     * Objects such as maps are created on-demand in Hazelcast. Touch all the one
     * we'll need to be sure they exist in advance, this doesn't change their
     * behaviour but is useful for reporting.
     * </p>
     */
    private void createNeededObjects() {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            this.jetInstance.getHazelcastInstance().getMap(iMapName);
        }
    }

    /**
     * <p>
     * Launch any "<i>system</i>" housekeeping jobs.
     * <p>
     */
    private void launchNeededJobs(boolean isLocalhost) {
    }

}
