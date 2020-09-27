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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer;

/**
 * <p>A source that consumes messages from Slack, by periodically polling
 * for messages with a greater timestamp than the last polling attempt.
 * </p>
 */
public class MySlackSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySlackSource.class);
    private static final long FOUR_SECONDS = 4L;
    private static final int MAX_ALLOWED_MESSAGES = 999;
    private static final long XXX = 1600000000L;

    private final HttpHeaders httpHeaders;
    private final String channelId;
    private final RestTemplate restTemplate;
    private long lastPoll;

    public MySlackSource(Properties properties) {
        this.channelId = properties.getProperty(MyConstants.SLACK_CHANNEL_ID);
        this.restTemplate = new RestTemplate();

        // Start to consume messages since launch
        this.lastPoll = XXX;
        //XXX Instant.now().getEpochSecond();

        this.httpHeaders = new HttpHeaders();
        this.httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.httpHeaders.setBearerAuth(properties.getProperty(MyConstants.SLACK_ACCESS_TOKEN));
    }

    /**
     * <p>Pass messages to Jet's buffer.
     * </p>
     */
    void fillBufferFn(SourceBuffer<String> buffer) {
        // Only poll every five seconds
        long now = Instant.now().getEpochSecond();
        if (now <= this.lastPoll + FOUR_SECONDS) {
            return;
        }

        // Handle messages in order, Slack gives reverse order
        SortedMap<Long, String> messages = this.pollSlack(this.lastPoll);
        messages.values().forEach(message -> buffer.add(message));

        this.lastPoll = now;
    }

    /**
     * <p>Poll Slack for messages since the last time we polled.
     * These are returned in reverse order. Slack doesn't seem
     * to prefer request params in the URL.
     * </p>
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    private SortedMap<Long, String> pollSlack(long since) {
        TreeMap<Long, String> results = new TreeMap<>();

        String url = SlackConstants.READ_MESSAGE_URL;
        url += "?channel=" + this.channelId;
        url += "&limit=" + MAX_ALLOWED_MESSAGES;
        url += "&oldest=" + this.lastPoll + ".000000";

        HttpEntity<String> requestEntity =
                new HttpEntity<>(this.httpHeaders);
        Map<String, String> params = new HashMap<>();

        try {
            ResponseEntity<Object> responseEntity =
                    this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, Object.class, params);

            // Response is a map
            Object body = responseEntity.getBody();
            if (responseEntity.getStatusCode() != HttpStatus.OK || body == null || !(body instanceof Map)) {
                String message = String.format("---- Receive from Slack fail ----%n => HTTP Status Code %d : %s%n => %s%n",
                        responseEntity.getStatusCodeValue(),
                        responseEntity.getStatusCode().getReasonPhrase(),
                        responseEntity);
                LOGGER.error(message);
            } else {
                Map<String, ?> bodyMap = (Map<String, ?>) body;
                // 'ok' should be a Boolean
                Object ok = bodyMap.get(SlackConstants.RESPONSE_KEY_OK);
                if (ok == null || !ok.toString().toLowerCase(Locale.ROOT).equals(Boolean.TRUE.toString())) {
                    String message = String.format("---- Receive from Slack fail ----%n => HTTP Status Code %d : %s%n => %s%n",
                            responseEntity.getStatusCodeValue(),
                            responseEntity.getStatusCode().getReasonPhrase(),
                            body);
                    LOGGER.error(message);
                } else {
                    Object messages = bodyMap.get(SlackConstants.RESPONSE_MESSAGES);
                    if (messages instanceof List) {
                        this.processList((List<Object>) messages, results);
                    } else {
                        String message = String.format("---- Receive from Slack fail ----%n => No %s%n => %s%n",
                                SlackConstants.RESPONSE_MESSAGES,
                                body);
                        LOGGER.error(message);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("pollSlack()", e);
        }

        return results;
    }

    /**
     * <p>Process one or more messages from Slack. Ignore any we can determine
     * to be ones we created, based on the presence or absence of "{@code bot_id}".
     * </p>
     *
     * @param messages
     * @param results
     */
    @SuppressWarnings("unchecked")
    private void processList(List<Object> messages, SortedMap<Long, String> results) {
        messages.forEach(message -> {
            if (message instanceof Map) {
                Map<String, String> messageMap = (Map<String, String>) message;
                String botId = messageMap.get(SlackConstants.RESPONSE_BOT_ID);
                String text = messageMap.get(SlackConstants.RESPONSE_TEXT);
                String timestampStr = messageMap.get(SlackConstants.RESPONSE_TIMESTAMP);
                // Ignore message with "bot_id", originated from us
                if (botId == null) {
                    // Granularity to nearest second, ignore fractions
                    String timestampSeconds = timestampStr.substring(0, timestampStr.indexOf('.'));
                    long timestamp = Long.parseLong(timestampSeconds);
                    results.put(timestamp, text);
                }
            } else {
                String errorMessage = String.format("---- Receive from Slack fail ----%n => No map%n => %s%n",
                        message);
                LOGGER.error(errorMessage);
            }
        });
    }

}
