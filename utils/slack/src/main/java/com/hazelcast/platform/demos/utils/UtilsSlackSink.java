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

package com.hazelcast.platform.demos.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Objects;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;

/**
 * <p>HTTP "{@code POST}" to send message to Slack.
 * Log on failure.
 * </p>
 */
public class UtilsSlackSink {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsSlackSink.class);

    private final String accessToken;
    private final String channelName;
    private final String messagePrefix;

    public UtilsSlackSink(String accessToken, String channelName, String projectName, String buildUser) {
        this.accessToken = Objects.toString(accessToken);
        this.channelName = Objects.toString(channelName);
        this.messagePrefix = "(_" + Objects.toString(projectName) + "/" + Objects.toString(buildUser) + "_): ";

        if (this.accessToken.length() < UtilsSlack.REASONABLE_MINIMAL_LENGTH_FOR_SLACK_PROPERTY) {
            String message = String.format("No Slack jobs, '%s' too short: '%s'",
                    UtilsConstants.SLACK_ACCESS_TOKEN, Objects.toString(this.accessToken));
            throw new RuntimeException(message);
        }
        if (this.channelName.length() < UtilsSlack.REASONABLE_MINIMAL_LENGTH_FOR_SLACK_PROPERTY) {
            String message = String.format("No Slack jobs, '%s' too short: '%s'",
                    UtilsConstants.SLACK_CHANNEL_ID, Objects.toString(this.channelName));
            throw new RuntimeException(message);
        }
    }

    /**
     * <p>A sink that does an HTTP Post to Slack.
     * </p>
     *
     * @param accessToken For access to Slack
     * @param channelName For stage name
     * @param projectName Prefix message so to know which demo produces
     * @param buildUser Who built, in case multiple people running same demo
     * @return
     */
    public static Sink<JSONObject> slackSink(String accessToken, String channelName, String projectName, String buildUser) {
        return SinkBuilder.sinkBuilder(
                    "slackSink-" + Objects.toString(channelName),
                    __ -> new UtilsSlackSink(accessToken, channelName, projectName, buildUser)
                )
                .receiveFn(
                        (UtilsSlackSink utilsSlackSink, JSONObject item) -> utilsSlackSink.receiveFn(item)
                        )
                .preferredLocalParallelism(1)
                .build();
    }

    /**
     * <p>Enrich a JSONObject with standard information and "{@code POST}" to
     * Slack.
     * </p>
     *
     * @param jsonObject
     * @return
     */
    public Object receiveFn(JSONObject jsonObject) {
        // Target channel goes in JSON message
        jsonObject.put(UtilsConstants.SLACK_PARAM_CHANNEL, this.channelName);

        String text = jsonObject.getString(UtilsConstants.SLACK_PARAM_TEXT);
        if (!text.startsWith(this.messagePrefix)) {
            jsonObject.put(UtilsConstants.SLACK_PARAM_TEXT, this.messagePrefix + text);
        }
        LOGGER.info("Sending to Slack: {}", jsonObject);

        try {
            HttpRequest httpRequest = buildHttpRequest(jsonObject.toString());
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> httpResponse =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == UtilsConstants.HTTP_OK && httpResponse.body() != null) {
                this.handleBody(httpResponse.body(), httpRequest.uri());
            } else {
                String message = String.format("%s:receiveFn response %d body '%s' for '%s'",
                        UtilsSlackSink.class.getSimpleName(),
                        httpResponse.statusCode(), Objects.toString(httpResponse.body()),
                        httpRequest.uri());
                LOGGER.error(message);
            }
        } catch (Exception e) {
            String message = String.format("%s:receiveFn",
                    UtilsSlackSink.class.getSimpleName()
                    );
            LOGGER.error(message, e);
        }

        return this;
    }

    /**
     * <p>Create a single use HTTP request.
     * </p>
     *
     * @return
     */
    private HttpRequest buildHttpRequest(String body) throws Exception {
        URI uri = new URI(UtilsConstants.SLACK_URL_WRITE_MESSAGE);

        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + this.accessToken)
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(BodyPublishers.ofString(body))
                .build();
    }

    /**
     * <p>Validate the response, was send ok ?
     * </p>
     */
    private void handleBody(String body, URI uri) {
        JSONObject json = new JSONObject(body);
        Boolean ok = json.getBoolean(UtilsConstants.SLACK_RESPONSE_KEY_OK);
        if (!ok) {
            String message = String.format("%s:handleBody '%s'=='%s' for %s",
                    UtilsSlackSink.class.getSimpleName(),
                    UtilsConstants.SLACK_RESPONSE_KEY_OK, ok, uri);
            LOGGER.error(message);
        }
    }

}
