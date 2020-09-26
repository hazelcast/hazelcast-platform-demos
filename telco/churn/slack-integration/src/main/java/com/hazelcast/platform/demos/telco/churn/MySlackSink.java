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

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * <p>Make a REST call to sink the object to the designated Slack destination.
 * </p>
 * <p>Kudos to Gokhan Oner, <a href="https://github.com/gokhanoner/hazelcast-jet-connectors">hazelcast-jet-connectors</a>
 * for the basis of this.
 * </p>
 */
public class MySlackSink {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySlackSink.class);
    private final String channelName;
    private final String token;
    private final RestTemplate restTemplate;

    public MySlackSink(Properties properties) {
        this.channelName = properties.getProperty(MyConstants.SLACK_CHANNEL_NAME);
        this.token = properties.getProperty(MyConstants.SLACK_ACCESS_TOKEN);
        this.restTemplate = new RestTemplate();
    }

    /**
     * <p>Take a JSON message, enrich with Slack connectity
     * and do an HTTP Post.
     * </p>
     *
     * @param jsonObject Message without delivery params
     */
    @SuppressWarnings("deprecation")
    public Object receiveFn(JSONObject jsonObject) {
        try {
            // Target channel goes in JSON message
            jsonObject.put(SlackConstants.CHANNEL, this.channelName);

            HttpHeaders headers = new HttpHeaders();
            //FIXME This is deprecated, but Slack seems to require it. Should be MediaType.APPLICATION_JSON
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.setBearerAuth(this.token);

            HttpEntity<String> request =
                    new HttpEntity<String>(jsonObject.toString(), headers);

            LOGGER.info("Sending to Slack: {}", jsonObject);

            ResponseEntity<Object> responseEntity
                = restTemplate.postForEntity(SlackConstants.POST_MESSAGE_URL, request, Object.class);

            Object body = responseEntity.getBody();
            if (responseEntity.getStatusCode() != HttpStatus.OK || body == null) {
                String message = String.format("---- Send to Slack fail ----%n => HTTP Status Code %d : %s%n => %s%n",
                        responseEntity.getStatusCodeValue(),
                        responseEntity.getStatusCode().getReasonPhrase(),
                        responseEntity);
                LOGGER.error(message);
            } else {
                try {
                    JSONObject responseBody = new JSONObject(body.toString());
                    boolean ok = responseBody.getBoolean("ok");
                    LOGGER.error("ok=={}", ok);
                    if (!ok) {
                        String message = String.format("---- Send to Slack fail ----%n => HTTP Status Code %d : %s%n => %s%n",
                                responseEntity.getStatusCodeValue(),
                                responseEntity.getStatusCode().getReasonPhrase(),
                                responseBody);
                        LOGGER.error(message);
                    }
                } catch (JSONException e2) {
                    /*FIXME Slack returns bad JSON on success!
                     */
                    if (e2.getMessage().startsWith("Unterminated object at character")) {
                        LOGGER.trace("Slack returns bad JSON on success: {}", body);
                    } else {
                        LOGGER.error("receiveFn()", e2);
                    }
                }
            }

        } catch (JSONException e) {
            LOGGER.error("receiveFn()", e);
        }

        return this;
    }

    /**
     * <p>Nothing to do, connection to Slack isn't kept open.
     * </p>
     */
    public Object destroyFn() {
        return this;
    }
}
