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

package hazelcast.platform.demos.industry.iiot;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.json.JSONObject;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>An {@link IMap} listener that sends new log messages (they are never updated)
 * to a web socket.
 * </p>
 */
@Slf4j
public class MySlf4jListener implements EntryAddedListener<HazelcastJsonValue, HazelcastJsonValue> {

    private SimpMessagingTemplate simpMessagingTemplate;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "A Spring singleton @Bean so must be shared")
    public MySlf4jListener(SimpMessagingTemplate arg0) {
        this.simpMessagingTemplate = arg0;
    }

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "new JSONObject(...) can throw exception")
    @Override
    public void entryAdded(EntryEvent<HazelcastJsonValue, HazelcastJsonValue> entryEvent) {
        log.trace("Map '{}' : {} => ('{}','{}')",
                entryEvent.getSource(),
                entryEvent.getEventType(),
                entryEvent.getKey(), entryEvent.getValue());
        try {
            JSONObject key = new JSONObject(entryEvent.getKey().toString());
            JSONObject value = new JSONObject(entryEvent.getValue().toString());

            LocalDateTime localDateTime =
                    Instant.ofEpochMilli(key.getLong(MyConstants.LOGGING_FIELD_TIMESTAMP))
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            String timestampStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);
            if (timestampStr.indexOf('.') > 0) {
                timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
            }

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ ");
            // Key fields, all of them
            stringBuilder.append(" \"" + MyConstants.LOGGING_FIELD_MEMBER_ADDRESS + "\": \""
                    + key.getString(MyConstants.LOGGING_FIELD_MEMBER_ADDRESS) + "\"");
            stringBuilder.append(", \"" + MyConstants.LOGGING_FIELD_TIMESTAMP + "\": \""
                    + timestampStr + "\"");
            // Value fields, some of them
            stringBuilder.append(", \"" + MyConstants.LOGGING_FIELD_LEVEL + "\": \""
                    + value.getString(MyConstants.LOGGING_FIELD_LEVEL).toUpperCase(Locale.ROOT) + "\"");
            stringBuilder.append(", \"" + MyConstants.LOGGING_FIELD_MESSAGE + "\": \""
                    + value.getString(MyConstants.LOGGING_FIELD_MESSAGE) + "\"");
            stringBuilder.append(" }");

            String json = stringBuilder.toString();
            String destination = MyLocalConstants.LOGGING_DESTINATION;

            log.debug("Sending to websocket '{}', {} bytes for {}", destination, json.length(), key);
            simpMessagingTemplate.convertAndSend(destination, json);
        } catch (Exception e) {
            log.error(entryEvent.toString(), e);
        }
    }
}
