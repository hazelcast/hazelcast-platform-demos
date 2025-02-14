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

package com.hazelcast.platform.demos.retail.clickstream.heartbeat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Emit a message periodicially, so that others know we are alive
 * (and across a WAN can deduce delivery speed).
 * </p>
 */
@Component
@EnableScheduling
@Slf4j
public class HeartbeatProducer {

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyProperties myProperties;

    /**
     * <p>Once a minute
     * </p>
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void heartbeat() {
        try {
            if (this.hazelcastInstance.getLifecycleService().isRunning()) {
                IMap<String, HazelcastJsonValue> iMap =
                        this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_HEARTBEAT);

                String key = this.hazelcastInstance.getConfig().getClusterName()
                        + "." + this.hazelcastInstance.getName();
                LocalDateTime now = LocalDateTime.now();
                long epochSecond = now.atZone(ZoneId.systemDefault()).toEpochSecond();
                String nowStr = DateTimeFormatter.ISO_DATE_TIME.format(now);

                String s = "{"
                        + "  \"site\": \"" + this.hazelcastInstance.getConfig().getClusterName() + "\""
                        + ", \"member\": \"" + this.hazelcastInstance.getName() + "\""
                        + ", \"address\": \"" + this.getAddress() + "\""
                        + ", \"id\": \"" + this.hazelcastInstance.getCluster().getLocalMember().getUuid() + "\""
                        + ", \"epoch_second\": " + epochSecond + ""
                        + ", \"heartbeat_timestamp\": \"" + nowStr + "\""
                        + ", \"build_timestamp\": \"" + this.myProperties.getBuildTimestamp() + "\""
                        + "}";

                HazelcastJsonValue value = new HazelcastJsonValue(s);

                // Logging listener sees all heartbeats, not just this. Only need highest diagnostics
                log.trace("'{}'.set('{}', '{}')", iMap.getName(), key, value);
                iMap.set(key, value);
            }
        } catch (Exception e) {
            log.error("heartbeat()", e);
        }
    }

    private String getAddress() {
        Member localMember = this.hazelcastInstance.getCluster().getLocalMember();
        Address address = localMember.getAddress();
        return address.getHost() + ":" + address.getPort();
    }
}
