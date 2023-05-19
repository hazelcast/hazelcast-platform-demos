/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private MyProperties myProperties;
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

        // Likely values, host isn't predictable for cloud
        configMap.replace(MyConstants.MARIA_DATABASE, MyConstants.CONFIG_VALUE_PLACEHOLDER,
                this.myProperties.getMariaDatabase());
        configMap.replace(MyConstants.MARIA_USERNAME, MyConstants.CONFIG_VALUE_PLACEHOLDER,
                this.myProperties.getMariaUsername());
        configMap.replace(MyConstants.MARIA_PASSWORD, MyConstants.CONFIG_VALUE_PLACEHOLDER,
                this.myProperties.getMariaPassword());
        configMap.replace(MyConstants.MONGO_COLLECTION1, MyConstants.CONFIG_VALUE_PLACEHOLDER,
                this.myProperties.getMongoCollection1());
        configMap.replace(MyConstants.MONGO_DATABASE, MyConstants.CONFIG_VALUE_PLACEHOLDER,
                this.myProperties.getMongoDatabase());
        configMap.replace(MyConstants.MONGO_USERNAME, MyConstants.CONFIG_VALUE_PLACEHOLDER,
                this.myProperties.getMongoUsername());
        configMap.replace(MyConstants.MONGO_PASSWORD, MyConstants.CONFIG_VALUE_PLACEHOLDER,
                this.myProperties.getMongoPassword());
    }

    /**
     * <p>Send existing values periodically to refresh web page
     * </p>
     * <p>Web does pull on first load, this is fallback in case this fails.
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
        try {
            String s = this.getAll();
            JSONObject json = new JSONObject(s);
            JSONArray payload = json.getJSONArray("payload");

            log.trace("sendAllConfig({}), existing items to send: {}", caller, payload.length());

            String destination = MyLocalConstants.CONFIG_DESTINATION;

            log.debug("Sending to websocket '{}', {}, {} bytes for {} items",
                    caller, destination, s.length(), payload.length());
            simpMessagingTemplate.convertAndSend(destination, s);
        } catch (Exception e) {
            log.error("sendAllConfig(): caller: " + caller, e);
        }
    }

    /**
     * <p>Complete set, formatted as JSON
     * </p>
     *
     * @return
     */
    private String getAll() {
        IMap<String, String> configMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_CONFIG);

        Set<String> keys = configMap.keySet().stream()
                .collect(Collectors.toCollection(TreeSet::new));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"payload\": [");

        if (!keys.isEmpty()) {
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
        }

        stringBuilder.append("] }");
        return stringBuilder.toString();
    }

    /**
     * <p>REST endpoint to retrieve all current values.
     * </p>
     *
     * @return
     */
    @GetMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public String get() {
        log.debug("get()");
        return this.getAll();
    }

    /**
     * <p>REST endpoint to set a key/value pair.
     * </p>
     *
     * @return
     */
    @GetMapping(value = "/set", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressFBWarnings(value = "RV_CHECK_FOR_POSITIVE_INDEXOF", justification = "only use if data before the colon")
    public String set(@RequestParam("the_key") String theKey,
            @RequestParam("the_value") String theValue) {
        log.debug("set('{}', '{}')", theKey, theValue);
        // If passed URL, extract host
        if (theValue.startsWith("http://") && theValue.length() > "http://".length()) {
            theValue = theValue.substring("http://".length());
            if (theValue.indexOf(":") > 0) {
                theValue = theValue.substring(0, theValue.indexOf(":"));
            }
            log.debug("set('{}', '{}') [amended]", theKey, theValue);
        }
        Object oldValue =
                this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_CONFIG).put(theKey, theValue);

        if (oldValue != null && theKey.contains("password")) {
            oldValue = "*hidden*";
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"the_key\": \"" + theKey + "\"");
        stringBuilder.append(", \"the_value\": \"" + theValue + "\"");
        stringBuilder.append(", \"the_old_value\": \"" + Objects.toString(oldValue) + "\"");
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }
}
