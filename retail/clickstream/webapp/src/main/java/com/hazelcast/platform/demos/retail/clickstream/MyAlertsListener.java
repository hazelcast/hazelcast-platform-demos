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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>An {@link IMap} listener that sends new alerts (they are never updated)
 * to a web socket.
 * </p>
 */
@Slf4j
public class MyAlertsListener implements EntryAddedListener<Long, String> {

    private SimpMessagingTemplate simpMessagingTemplate;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "A Spring singleton @Bean so must be shared")
    public MyAlertsListener(SimpMessagingTemplate arg0) {
        this.simpMessagingTemplate = arg0;
    }

    @Override
    public void entryAdded(EntryEvent<Long, String> entryEvent) {
        log.trace("Map '{}' : {} => ('{}','{}')",
                entryEvent.getSource(),
                entryEvent.getEventType(),
                entryEvent.getKey(), entryEvent.getValue());
        try {
            LocalDateTime localDateTime =
                    Instant.ofEpochMilli(entryEvent.getKey()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            String timestampStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);

            if (timestampStr.indexOf('.') > 0) {
                timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
            }

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ ");
            stringBuilder.append(" \"timestamp\": \"" + timestampStr + "\"");
            stringBuilder.append(", \"payload\": \"" + entryEvent.getValue() + "\"");
            stringBuilder.append(" }");

            String json = stringBuilder.toString();
            String destination = MyLocalConstants.ALERTS_DESTINATION;

            log.debug("Sending to websocket '{}', message {} bytes", destination, json);
            simpMessagingTemplate.convertAndSend(destination, json);
        } catch (Exception e) {
            log.error(entryEvent.toString(), e);
        }
    }
}
