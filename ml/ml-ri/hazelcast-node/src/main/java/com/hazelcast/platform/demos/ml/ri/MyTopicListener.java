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

package com.hazelcast.platform.demos.ml.ri;

import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

/**
 * <p>Listener on a topic and produce output, with a banner above and
 * below to make it more eye-catching.
 * </p>
 */
public class MyTopicListener implements MessageListener<Object> {

    private static final String NEWLINE = System.getProperty("line.separator");
    private static final String FOUR_SPACES = "    ";

    /**
     * <p>Called per message, we are passed the topic name and the
     * message object. We assume the message object is a string, with
     * the Jet job name as the first comma separated parameter.
     * </p>
     */
    @Override
    public void onMessage(Message<Object> message) {
        String text = message.getMessageObject().toString();
        int comma = text.indexOf(',');

        String output;
        if (comma > 0) {
            output = String.format("Topic '%s' : Job '%s' : Value '%s'",
                    message.getSource(), text.substring(0, comma), text.substring(comma + 1));
        } else {
            output = String.format("Topic '%s' : Message '%s'",
                    message.getSource(), text);
        }

        String[] results = new String[] { NEWLINE,
                FOUR_SPACES + "***************************************************************" + FOUR_SPACES,
                FOUR_SPACES + output + FOUR_SPACES,
                FOUR_SPACES + "***************************************************************" + FOUR_SPACES, };

        StringBuilder sb = new StringBuilder();
        for (String line : results) {
            sb.append(line).append(NEWLINE);
        }

        System.out.println(sb.toString());
    }
}
