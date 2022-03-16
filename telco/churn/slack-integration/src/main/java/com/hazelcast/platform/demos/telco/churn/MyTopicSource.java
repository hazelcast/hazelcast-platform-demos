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

package com.hazelcast.platform.demos.telco.churn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Create an infinite stream source by listening for messages
 * published to a Hazelcast {@link com.hazelcast.topic.ITopic ITopic}.
 * </p>
 */
public class MyTopicSource implements MessageListener<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyTopicSource.class);
    private static final int QUEUE_CAPACITY = 1_000;

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final String member;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
            justification = "Cannot change addMessageListener()")
    public MyTopicSource(ITopic<String> topic, String member) {
        topic.addMessageListener(this);
        this.member = member;
    }

    /**
     * <p>When a message is received from the topic, add it to the
     * local buffer.
     * <p>
     */
    @Override
    public void onMessage(Message<String> message) {
        String messageObject = message.getMessageObject();
        messageObject += ", via " + member;
        boolean success = this.queue.offer(messageObject);
        if (!success) {
            LOGGER.error("Dropped message, buffer full, '{}'", messageObject);
        }
    }

    /**
     * <p>Pass messages from the local buffer to Jet's buffer.
     * </p>
     */
    void fillBufferFn(SourceBuffer<String> buffer) {
        if (this.queue.peek() != null) {
            List<String> list = new ArrayList<>();
            this.queue.drainTo(list);
            list.forEach(item -> buffer.add(item));
        }
    }

}
