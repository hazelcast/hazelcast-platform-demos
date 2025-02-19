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

package com.hazelcast.platform.demos.telco.churn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

/**
 * <p>Feed data from a Hazelcast {@link com.hazelcast.topic.ITopic} into a
 * web socket.
 * </p>
 */
@Component
public class MySocketTopicListener implements MessageListener<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySocketTopicListener.class);

    private static final String DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_TOPICS_PREFIX
            + "/" + MyConstants.ITOPIC_NAME_SLACK;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    /**
     * <p>Some diagnostics to log.
     * </p>
     */
    public MySocketTopicListener() {
        LOGGER.trace("Destination: {}", DESTINATION);
    }

    /**
     * <p>
     * Republish from Hazelcast's {@code com.hazelcast.topic.ITopic Topic} to the
     * web socket.
     * </p>
     */
    @Override
    public void onMessage(Message<String> message) {

        String payload = message.getMessageObject();

        long now = System.currentTimeMillis();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"payload\": \"");
        stringBuilder.append(MyUtils.safeForJsonStr(payload));
        stringBuilder.append("\", \"now\": \"" + now + "\"");
        stringBuilder.append(" }");

        String result = stringBuilder.toString();
        // Debug not trace as an alert
        LOGGER.debug("Sending to websocket '{}'", result);

        this.simpMessagingTemplate.convertAndSend(DESTINATION, result);
    }

}
