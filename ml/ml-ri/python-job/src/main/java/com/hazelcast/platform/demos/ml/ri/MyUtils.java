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

package com.hazelcast.platform.demos.ml.ri;

import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;

/**
 * <p>Utility functions to share between {@link Pi1Job} and {@link Pi2Job}.
 * </p>
 */
public class MyUtils {

    /**
     * <p>Publish the event to a topic for all listeners to be aware
     * of.
     * </p>
     *
     * @param klass - Sending job class name.
     * @param topicName - Topic name, should always be "{@code pi}".
     */
    protected static Sink<? super String> buildTopicSink(Class<?> klass, String topicName) {
        return SinkBuilder.sinkBuilder(
                        "topicSink-" + topicName,
                        context -> context.jetInstance().getHazelcastInstance().getTopic(topicName)
                        )
                        .receiveFn((iTopic, item) ->
                            iTopic.publish(klass.getSimpleName() + "," + item.toString()))
                        .build();
    }

}
