/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer;

/**
 * <p>Create a continuous stream of strings from a single Slack
 * channel, by periodically polling for messages
 * </p>
 * <p>It is unclear how frequently Slack can be polled without
 * the BOT being classed as a DOS attack. Four seconds seems
 * to have worked ok so far.
 * </p>
 */
public class UtilsSlackSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsSlackSource.class);
    private static final long FOUR_SECONDS = 4L;
    private static final int MAX_ALLOWED_MESSAGES = 999;

    private final String accessToken;
    private final String channelId;
    private long lastPoll;

    public UtilsSlackSource(String accessToken, String channelId) {
        this.accessToken = accessToken;
        this.channelId = channelId;

        // Start to consume messages since launch
        this.lastPoll = Instant.now().getEpochSecond();
    }

    /**
     * <p>Create a continuous source listening to Slack.
     * </p>
     *
     * @param accessToken For access to Slack
     * @param channelId For access to Slack
     * @param channelName For job name
     * @return
     */
    public static StreamSource<String> slackSource(String accessToken, String channelId, String channelName) {
        return SourceBuilder.stream(
                    "slackSource-" + channelName,
                    __ -> {
                        return new UtilsSlackSource(accessToken, channelId);
                    }
                )
                .fillBufferFn(UtilsSlackSource::fillBufferFn)
                .build();
    }

    /**
     * <p>Polling loop, only actually call Slack every few seconds
     * </p>
     */
    public void fillBufferFn(SourceBuffer<String> buffer) {
        long now = Instant.now().getEpochSecond();
        if (now <= this.lastPoll + FOUR_SECONDS) {
            return;
        }

        // Handle messages in time order
        SortedMap<Long, String> messages = this.pollSlack(this.lastPoll);
        messages.values().forEach(message -> buffer.add(message));

        this.lastPoll = now;
    }

    /**
     * <p>Request messages since last poll.
     * </p>
     * <p>Slack returns messages in reverse order (newest first).
     * Rearrange to oldest first, although this is arbitrary since
     * messages should be independent and we don't expect there
     * to be many per poll, usually one or none.
     * </p>
     *
     * @return Possibly none but not null map
     */
    private SortedMap<Long, String> pollSlack(long since) {
        TreeMap<Long, String> results = new TreeMap<>();

        try {
            HttpRequest httpRequest = buildHttpRequest(since);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> httpResponse =
                  httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == UtilsConstants.HTTP_OK && httpResponse.body() != null) {
                this.handleBody(httpResponse.body(), results, httpRequest.uri());
            } else {
                String message = String.format("%s:pollSlack response %d body '%s' for '%s'",
                        UtilsSlackSource.class.getSimpleName(),
                        httpResponse.statusCode(), Objects.toString(httpResponse.body()),
                        httpRequest.uri());
                LOGGER.error(message);
            }
        } catch (Exception e) {
            String message = String.format("%s:pollSlack",
                    UtilsSlackSource.class.getSimpleName()
                    );
            LOGGER.error(message, e);
        }
        return results;
    }

    /**
     * <p>Create a single use HTTP request.
     * </p>
     *
     * @param since Oldest timestamp to be returned
     * @return
     */
    private HttpRequest buildHttpRequest(long since) throws Exception {
        String timestampMicroseconds = since + ".000000";
        String url = String.format("%s?channel=%s&limit=%s&oldest=%s",
                UtilsConstants.SLACK_URL_READ_MESSAGE,
                this.channelId,
                MAX_ALLOWED_MESSAGES,
                timestampMicroseconds);
        URI uri = new URI(url);

        return HttpRequest.newBuilder(uri)
               .header("Authorization", "Bearer " + this.accessToken)
               .header("Content-Type", "application/x-www-form-urlencoded")
               .build();
    }

    /**
     * <p>The body will have one or more messages. We want the
     * text and timestamp for each. Exclude messages with "{@code bot_id}"
     * as these are ones send by the code, answers not questions.
     * </p>
     *
     * @param body
     * @param results
     */
    private void handleBody(String body, SortedMap<Long, String> results, URI uri) {
        JSONObject json = new JSONObject(body);
        Boolean ok = json.getBoolean(UtilsConstants.SLACK_RESPONSE_KEY_OK);
        if (ok) {
            JSONArray messages = json.getJSONArray(UtilsConstants.SLACK_RESPONSE_MESSAGES);
            for (int i = 0 ; i < messages.length() ; i++) {
                JSONObject message = messages.getJSONObject(i);
                if (!message.has(UtilsConstants.SLACK_RESPONSE_BOT_ID)) {
                    String text = message.getString(UtilsConstants.SLACK_RESPONSE_TEXT);
                    String timestampStr = message.getString(UtilsConstants.SLACK_RESPONSE_TIMESTAMP);
                    String timestampSeconds = timestampStr.substring(0, timestampStr.indexOf('.'));
                    long timestamp = Long.parseLong(timestampSeconds);
                    results.put(timestamp, text);
                }
            }
        } else {
            String message = String.format("%s:handleBody '%s'=='%s' for %s",
                    UtilsSlackSource.class.getSimpleName(),
                    UtilsConstants.SLACK_RESPONSE_KEY_OK, ok, uri);
            LOGGER.error(message);
        }
    }

}
