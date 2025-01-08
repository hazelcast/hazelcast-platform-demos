/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Kick off some listeners then mainly leave it up to React.js
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationRunner {
    private static final int FIVE = 5;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Value("${spring.application.name}")
    private String springApplicationName;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                this.hazelcastInstance.getName());
            log.info("${spring.applicationName}=='{}'", this.springApplicationName);

            this.addLifecycleListener();

            // On demand
            MyAlertsListener myAlertsListener = new MyAlertsListener(this.simpMessagingTemplate);
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_ALERT)
            .addEntryListener(myAlertsListener, true);

            // Send periodically to React in case socket notification missed (eg. at startup)
            while (this.hazelcastInstance.getLifecycleService().isRunning()) {
                this.sendCluster();
                TimeUnit.SECONDS.sleep(FIVE);
            }

            // Reached if shutdown from Management Center
            log.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-",
                    this.hazelcastInstance.getName());
        };
    }
    /**
     * <p>List to republished cluster events to a websocket for React.js
     * </p>
     */
    private void addLifecycleListener() {
        MySocketFailoverListener mySocketFailoverListener =
                new MySocketFailoverListener(this.simpMessagingTemplate);
        this.hazelcastInstance.getLifecycleService().addLifecycleListener(mySocketFailoverListener);
    }

    /**
     * <p>Send selected cluster events to a websocket for React.js
     * </p>
     */
    private void sendCluster() {
        String message1 =  MyLocalUtils.clusterNamePayload();
        String message2 =  MyLocalUtils.clusterMembersPayload();
        String message3 =  this.dataPayload();
        String destination1 = MyLocalConstants.CLUSTER_DESTINATION;
        String destination2 = MyLocalConstants.MEMBERS_DESTINATION;
        String destination3 = MyLocalConstants.DATA_DESTINATION;

        try {
            log.trace("Sending to websocket '{}', message {}", destination1, message1);
            this.simpMessagingTemplate.convertAndSend(destination1, message1);
        } catch (Exception e) {
            log.error("sendCluster():" + destination1, e);
        }
        try {
            log.trace("Sending to websocket '{}', message {}", destination2, message2);
            this.simpMessagingTemplate.convertAndSend(destination2, message2);
        } catch (Exception e) {
            log.error("sendCluster():" + destination2, e);
        }
        try {
            log.trace("Sending to websocket '{}', message {}", destination3, message3);
            this.simpMessagingTemplate.convertAndSend(destination3, message3);
        } catch (Exception e) {
            log.error("sendCluster():" + destination3, e);
        }
    }

    /**
     * <p>Create a list of Strings, comma separated pairs
     * </p>
     */
    private String dataPayload() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ ");
        stringBuilder.append(" \"payload\": [");

        stringBuilder.append(" \"TIMESTAMP," + this.getTimestamp() + "\"");
        this.appendSizes(stringBuilder);

        stringBuilder.append("]");
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    /**
     * <p>Format date for presentation
     * </p>
     */
    private String getTimestamp() {
        long now = System.currentTimeMillis();
        LocalDateTime localDateTime =
                Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDateTime();
        String timestampStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);

        if (timestampStr.indexOf('.') > 0) {
            timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
        }

        return timestampStr;
    }

    /**
     * <p>Duplicates Management Center, map sizes.
     * </p>
     *
     * @param stringBuilder
     */
    private void appendSizes(StringBuilder stringBuilder) {
        Set<String> names =
                this.hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> distributedObject instanceof IMap)
                .map(distributedObject -> distributedObject.getName())
                .filter(name -> !name.startsWith("__"))
                .collect(Collectors.toCollection(TreeSet::new));

        for (String name : names) {
            stringBuilder.append(", \"" + name + ","
                    + String.valueOf(
                            this.hazelcastInstance.getMap(name).size())
                    + " entries\"");
        }
    }

}
