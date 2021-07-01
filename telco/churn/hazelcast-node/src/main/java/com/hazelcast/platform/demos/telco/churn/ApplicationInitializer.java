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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.telco.churn.mapstore.UpdatedByMapInterceptor;
import com.hazelcast.topic.ITopic;
import com.hazelcast.platform.demos.telco.churn.mapstore.MyMapHelpers;

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
    private HazelcastInstance hazelcastInstance;
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

            int currentSize = this.hazelcastInstance.getCluster().getMembers().size();
            if (this.myProperties.getInitSize() != currentSize) {
                LOGGER.info("Cluster size {}, initializing at {}", currentSize, this.myProperties.getInitSize());
            } else {
                LOGGER.info("Cluster size {}, -=-=-=-=- START initialize by '{}' START -=-=-=-=-=-",
                        currentSize, this.hazelcastInstance.getName());
                var bootstrapServers = this.myProperties.getBootstrapServers();
                LOGGER.debug("Kafka brokers: {}", bootstrapServers);
                // Create maps before defining their metadata
                this.createNeededObjects();
                // For SQL against Kafka
                this.defineKafka(bootstrapServers);
                // For SQL against empty Imap
                this.defineIMap();
                // Do jobs last, in case they use SQL
                this.launchNeededJobs(isLocalhost);
                LOGGER.info("Cluster size {}, -=-=-=-=-  END  initialize by '{}'  END  -=-=-=-=-=-",
                        currentSize, this.hazelcastInstance.getName());
            }
        };
    }

    /**
     * <p>
     * Objects such as maps are created on-demand in Hazelcast. Touch all the one
     * we'll need to be sure they exist in advance, this doesn't change their
     * behaviour but is useful for reporting.
     * </p>
     * <p>Intercept changes to the call data record and customer maps, to ensure the
     * "{@code lastModifiedBy}" and "{@code lastModifiedDate}" are set
     * on writes.
     * </p>
     */
    private void createNeededObjects() {
        String modifiedBy = MyMapHelpers.getModifiedBy(this.myProperties);
        for (String iMapName : MyConstants.CDC_MAPSTORE_NAMES) {
            IMap<?, ?> iMap =
                    this.hazelcastInstance.getMap(iMapName);
            iMap.addInterceptor(new UpdatedByMapInterceptor(modifiedBy));
        }

        for (String iMapName : MyConstants.IMAP_NAMES) {
            this.hazelcastInstance.getMap(iMapName);
        }
        for (String iTopicName : MyConstants.ITOPIC_NAMES) {
            ITopic<Object> iTopic =
                    this.hazelcastInstance.getTopic(iTopicName);
            // Log on the topics added
            iTopic.addMessageListener(new MyLoggingTopicListener());
        }
    }

    /**
     * <p>
     * Launch any "<i>system</i>" housekeeping jobs.
     * <p>
     */
    private void launchNeededJobs(boolean isLocalhost) {
    }

    /**
     * <p>Define Kafka streams so can be directly used as a
     * querying source by SQL.
     * </p>
     *
     * @param bootstrapServers
     */
    private void defineKafka(String bootstrapServers) {
        // Since only run once, don't need 'CREATE OR REPLACE'
        String definition1 = "CREATE EXTERNAL MAPPING "
                + MyConstants.KAFKA_TOPIC_CALLS_NAME
                + " ( "
                + " id           VARCHAR, "
                + " callerTelno  VARCHAR, "
                + " callerMastId VARCHAR, "
                + " calleeTelno  VARCHAR, "
                + " calleeMastId VARCHAR, "
                + " startTimestamp BIGINT, "
                + " durationSeconds INTEGER, "
                + " callSuccessful BOOLEAN, "
                + " createdBy VARCHAR, "
                + " createdDate BIGINT, "
                + " lastModifiedBy VARCHAR, "
                + " lastModifiedDate BIGINT "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'json',"
                + " 'valueFormat' = 'json',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";
        this.define(definition1);
    }

    /**
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     */
    private void defineIMap() {
        // Since only run once, don't need 'CREATE OR REPLACE'
        String definition1 = "CREATE MAPPING "
                + MyConstants.IMAP_NAME_SENTIMENT
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'portable',"
                + " 'valueJavaClass' = 'com.hazelcast.platform.demos.telco.churn.domain.Sentiment',"
                + " 'valuePortableFactoryId' = '" + MyConstants.CLASS_ID_MYPORTABLEFACTORY + "',"
                + " 'valuePortableClassId' = '" + MyConstants.CLASS_ID_SENTIMENT + "'"
                + " )";
        this.define(definition1);
    }

    /**
     * <p>Generic handler to loading definitions
     * </p>
     *
     * @param definition
     */
    private void define(String definition) {
        LOGGER.trace("Definition '{}'", definition);
        try {
            this.hazelcastInstance.getSql().execute(definition);
        } catch (Exception e) {
            LOGGER.error(definition, e);
        }
    }

}
