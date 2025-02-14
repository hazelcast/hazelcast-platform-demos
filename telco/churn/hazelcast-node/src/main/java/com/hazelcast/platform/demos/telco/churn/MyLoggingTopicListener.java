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

import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

/**
 * <p>Log messages received, to help confirm publishing is working.
 * </p>
 */
public class MyLoggingTopicListener implements MessageListener<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyLoggingTopicListener.class);
    private static final String NEWLINE = System.getProperty("line.separator");
    private static final String FOUR_SPACES = "    ";

    /**
     * <p>Log the message, with padding to keep it apart from other
     * log messages.
     * </p>
     */
    @Override
    public void onMessage(Message<Object> message) {
        String outputMessage =
                String.format("Topic '%s' : %s", message.getSource(), message.getMessageObject());

        String[] output = new String[] {
                NEWLINE,
                FOUR_SPACES + "***************************************************************"
                    + FOUR_SPACES,
                FOUR_SPACES + outputMessage + FOUR_SPACES,
                FOUR_SPACES + "***************************************************************"
                    + FOUR_SPACES,
        };

        StringBuilder sb = new StringBuilder();
        for (String line : output) {
            sb.append(line).append(NEWLINE);
        }
        LOGGER.info(NEWLINE + sb.toString() + NEWLINE);
   }

}
