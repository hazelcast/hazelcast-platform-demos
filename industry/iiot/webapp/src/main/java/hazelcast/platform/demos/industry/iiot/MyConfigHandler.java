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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>An {@link IMap} listener that sends new config data, which mainly append only,
 * to a web socket.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/rest/config")
public class MyConfigHandler implements EntryAddedListener<String, String>, EntryUpdatedListener<String, String> {

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void entryAdded(EntryEvent<String, String> entryEvent) {
        log.trace("Map '{}' : {} => ('{}','{}')",
                entryEvent.getSource(),
                entryEvent.getEventType(),
                entryEvent.getKey(), entryEvent.getValue());
        this.sendAllConfig(entryEvent.getEventType() + ":" + entryEvent.getKey());
    }
    @Override
    public void entryUpdated(EntryEvent<String, String> entryEvent) {
        log.trace("Map '{}' : {} => ('{}','{}')",
                entryEvent.getSource(),
                entryEvent.getEventType(),
                entryEvent.getKey(), entryEvent.getValue());
        this.sendAllConfig(entryEvent.getEventType() + ":" + entryEvent.getKey());
    }

    /**
     * <p>Ensure some value for config exists, until propertly set.
     * </p>
     */
    public void initConfig() {
        IMap<String, String> configMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_CONFIG);

        for (String key : MyConstants.CONFIG_REQUIRED) {
            configMap.putIfAbsent(key, MyConstants.CONFIG_VALUE_PLACEHOLDER);
        }
        for (String key : MyConstants.CONFIG_OPTIONAL) {
            configMap.putIfAbsent(key, MyConstants.CONFIG_VALUE_PLACEHOLDER);
        }
    }

    /**
     * <p>Send existing values periodically to refresh web page
     * </p>
     */
    public void pushConfig() {
        this.sendAllConfig("pushConfig()");
    }

    /**
     * <p>Send all config, periodically or in reaction to change.
     * </p>
     *
     * @param caller
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "new JSONObject(...) can throw exception")
    private void sendAllConfig(String caller) {
        IMap<String, String> configMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_CONFIG);

        Set<String> keys = configMap.keySet().stream()
                .collect(Collectors.toCollection(TreeSet::new));

        log.trace("sendAllConfig({}), existing items to send: {}", caller, keys.size());
        if (!keys.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ \"payload\": [");

            int count = 0;
            for (String key : keys) {
                if (count > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append("{");
                stringBuilder.append(" \"key\": \"" + key + "\"");
                String value = configMap.get(key);
                if (key.toLowerCase(Locale.ROOT).contains("password")) {
                    StringBuffer valueObscuredStringBuffer = new StringBuffer();
                    for (int i = 0; i < value.length(); i++) {
                        valueObscuredStringBuffer.append("*");
                    }
                    stringBuilder.append(", \"value\": \"" + valueObscuredStringBuffer.toString() + "\"");
                } else {
                    stringBuilder.append(", \"value\": \"" + value + "\"");
                }

                // Last updated
                EntryView<String, String> entryView = configMap.getEntryView(key);
                String timestampStr = "?";
                if (entryView != null && entryView.getLastUpdateTime() > 0) {
                    long timestamp = entryView.getLastUpdateTime();
                    LocalDateTime localDateTime =
                            Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime();
                    timestampStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);
                    if (timestampStr.indexOf('.') > 0) {
                        timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
                    }
                }

                stringBuilder.append(", \"since\": \"" + timestampStr + "\"");
                stringBuilder.append("}");

                count++;
            }

            stringBuilder.append("] }");

            try {
                String json = stringBuilder.toString();
                String destination = MyLocalConstants.CONFIG_DESTINATION;

                log.debug("Sending to websocket '{}', {}, {} bytes for {} items",
                        caller, destination, json.length(), count);
                simpMessagingTemplate.convertAndSend(destination, json);
            } catch (Exception e) {
                log.error("sendAllConfig(): caller: " + caller, e);
            }
        }
    }
}
