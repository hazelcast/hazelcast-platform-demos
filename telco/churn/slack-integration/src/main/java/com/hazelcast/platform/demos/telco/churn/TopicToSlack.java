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

import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cluster.Address;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.utils.UtilsConstants;
import com.hazelcast.platform.demos.utils.UtilsSlackSink;
import com.hazelcast.topic.ITopic;

/**
 * <p>A three step Jet job, read from one place and write to another. We
 * re-format in the intermediate stage but don't filter.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                |  Topic Source   |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                | Format as JSON  |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 3 )------+
 *                |   Slack Sink    |
 *                +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * The Topic source.
 * </p>
 * <p>Consume messages published to a Hazelcast {@link com.hazelcast.topic.ITopic}.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformat.
 * </p>
 * <p>Convert a String into JSON.
 * </p>
 * </li>
 * <li>
 * <p>
 * The Slack sink.
 * </p>
 * <p>Use HTTP to sent a REST message with JSON content to the Slack API.
 * </p>
 * </li>
 * </ol>
 */
public class TopicToSlack {
    public static final String JOB_NAME_PREFIX = TopicToSlack.class.getSimpleName();
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicToSlack.class);

    /**
     * <p>Straight-forward connectivity, read from a topic and pass everything
     * to the Slack cannel.
     * </p>
     */
    public static Pipeline buildPipeline(Properties properties, String topicName, String projectName) {

        Pipeline pipeline = Pipeline.create();

        StreamStage<JSONObject> readAndMap =
                pipeline
                .readFrom(TopicToSlack.myTopicSource(topicName)).withoutTimestamps()
                .map(TopicToSlack.myMapStage()).setName("reformat-to-JSON");

        // If Slack integration not available, log to console instead
        if (properties.get(MyConstants.SLACK_ACCESS_TOKEN) == null
                || properties.get(MyConstants.SLACK_CHANNEL_ID) == null
                || properties.get(MyConstants.SLACK_CHANNEL_NAME) == null) {
            LOGGER.error("No Slack connection properties, alerting will be to stdout");
            readAndMap
            .writeTo(Sinks.logger());
        } else {
            String accessToken = properties.getProperty(MyConstants.SLACK_ACCESS_TOKEN);
            String buildUser = properties.getProperty(UtilsConstants.SLACK_BUILD_USER);
            String channel = properties.getProperty(MyConstants.SLACK_CHANNEL_NAME);

            readAndMap
            .writeTo(UtilsSlackSink.slackSink(accessToken, channel, projectName, buildUser));
        }

        return pipeline;
    }

    /**
     * <p>Create a streaming source of String objects, using a
     * topic listener.
     * </p>
     *
     * @param topicName
     * @return
     */
    private static StreamSource<String> myTopicSource(String topicName) {
        return SourceBuilder.stream(
                    "topicSource-" + topicName,
                    context -> {
                        ITopic<String> topic
                            = context.hazelcastInstance().getTopic(topicName);
                        Address address = context.hazelcastInstance().getCluster().getLocalMember().getAddress();
                        String member = address.getHost() + ":" + address.getPort();
                        return new MyTopicSource(topic, member);
                    }
                )
                .fillBufferFn(MyTopicSource::fillBufferFn)
                .build();
    }

    /**
     * <p>Reformat the incoming text to a JSON object for Slack.
     * </p>
     * <p>Use internal rather than global constants, only relevant for Slack.
     * </p>
     */
    private static FunctionEx<String, JSONObject> myMapStage() {
        return str -> {
            String cleanStr = str.replaceAll("\"", "'");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(UtilsConstants.SLACK_PARAM_TEXT, cleanStr);

            return jsonObject;
        };
    }

}
