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
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple2;

//FIXME import org.slf4j.Logger;
//FIXME import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
//FIXME import com.hazelcast.sql.SqlResult;

/**
 * <p>A job to process "{@code CLI}" messages from Slack
 * and respond back to Slack. Currently only SQL is
 * handled.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                |  Slack Source   |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                |Determine handled|
 *                +-----------------+
 *                      /     \
 *                     /       \
 *                    /         \
 *  +------( 3 )------+         +------( 4 )------+
 *  |    Rejected     |         |     SQL Call    |
 *  +-----------------+         +-----------------+
 *                    \         /
 *                     \       /
 *                      \     /
 *                +------( 5 )------+
 *                | Format as JSON  |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 6 )------+
 *                |  Slack Source   |
 *                +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * Slack source
 * </p>
 * <p>Read recent messages from a Slack channel. Discard any sent by us.
 * </p>
 * </li>
 * <li>
 * <p>
 * Determination switch
 * </p>
 * <p>Decide if we can attempt to parse this message (right leg) or not
 * (left leg).
 * </p>
 * </li>
 * <li>
 * <p>
 * Left leg - reject
 * </p>
 * <p>Format output for an input message we know we cannot handle.
 * </p>
 * </li>
 * <li>
 * <p>
 * Right leg - try to accept
 * </p>
 * <p>Try to process the input, returning the result or an error.
 * </p>
 * </li>
 * <li>
 * <p>
 * Format
 * </p>
 * <p>Prepare JSON for sending to Slack.
 * </p>
 * </li>
 * <li>
 * <p>
 * Slack sink
 * </p>
 * <p>Write input back to the same Slack channel we read from.
 * </p>
 * </li>
 * </ol>
 */
public class SlackToSlackCLI {
    public static final String JOB_NAME_PREFIX = SlackToSlackCLI.class.getSimpleName();
    //FIXME private static final Logger LOGGER = LoggerFactory.getLogger(SlackToSlackCLI.class);

    private static final String SELECT = "SELECT";

    /**
     * <p>Run an asynchronous query. Jet here is in embedded mode so runs
     * server-side. To see Jet calls authenticated and authorised, use Jet
     * in client-server mode.
     * </p>
     */
    private static BiFunctionEx<? super HazelcastInstance, ? super Tuple2<Boolean, String>, ? extends CompletableFuture<String>>
        mapAsyncSqlFn() {
        return (hazelcastInstance, tuple2) -> {
            return CompletableFuture.supplyAsync(new Supplier<String>() {
                @Override
                public String get() {
                    String query = MyUtils.makeUTF8(tuple2.f1());

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("```");
                    stringBuilder.append("Query: ").append(query).append(MyUtils.NEWLINE);

                    /*FIXME needs IMDG 4.1
                    try {
                        SqlResult sqlResult = hazelcastInstance.getSql().execute(query);

                        stringBuilder.append(MyUtils.prettyPrintSqlResult(sqlResult));
                    } catch (Exception e) {
                        LOGGER.error("Query: {} gave {}", tuple2.f1(), e.getMessage());
                        stringBuilder.append("FAILED: ").append(e.getMessage()).append(MyUtils.NEWLINE);
                    }*/

                    stringBuilder.append("```");
                    return stringBuilder.toString();
                }
            });
        };
    }


    /**
     * <p>Create a Slack read, process, Slack write pipeline. Here we can assume
     * Slack credentials are ok, all that might go wrong is a parse error on
     * processing the human's input.
     * </p>
     */
    public static Pipeline buildPipeline(Properties properties, String projectName) {
        String channel = properties.getProperty(MyConstants.SLACK_CHANNEL_NAME);

        ServiceFactory<?, HazelcastInstance> hazelcastInstanceService =
                ServiceFactories.sharedService(ctx -> ctx.jetInstance().getHazelcastInstance());

        Pipeline pipeline = Pipeline.create();

        StreamStage<String> streamSource =
            pipeline
            .readFrom(SlackToSlackCLI.mySlackSource(properties)).withoutTimestamps();

        // Use the the first word to determine if it is an SQL statement
        StreamStage<Tuple2<Boolean, String>> possibleSqlStatement =
            streamSource
            .map(str -> {
                Boolean handled = str.startsWith(SELECT);
                return Tuple2.tuple2(handled, str);
            }).setName("determine-if-handled");

        // Branch for unhandled input type
        StreamStage<String> unhandledInput =
                possibleSqlStatement
                .filter(tuple2 -> !tuple2.f0())
                .map(tuple2 -> {
                    return "Sorry, only '" + SELECT + "' commands handled, not '" + tuple2.f1() + "'";
                })
                .setName("not-an-sql-statement");

        // Branch for handled input type, throttle to 1 thread
        StreamStage<String> handledInput =
                possibleSqlStatement
                .filter(tuple2 -> tuple2.f0())
                .mapUsingServiceAsync(hazelcastInstanceService, mapAsyncSqlFn())
                .setLocalParallelism(1)
                .setName("is-an-sql-statement");

        // Join the handled and unhandled, and return to Slack
        unhandledInput.merge(handledInput).setName("merge-handled-and-unhandled")
        .map(SlackToSlackCLI.myMapStage()).setName("reformat-to-JSON")
        .writeTo(SlackToSlackCLI.mySlackChannelSink(channel, properties, projectName));

        return pipeline;
    }


    /**
     * <p>Create a streaming source of String objects, using a
     * connectivity to Slack
     * </p>
     *
     * @param channelName
     * @return
     */
    private static StreamSource<String> mySlackSource(Properties properties) {
        return SourceBuilder.stream(
                    "slackSource-" + properties.getProperty(MyConstants.SLACK_CHANNEL_NAME),
                    context -> {
                        return new MySlackSource(properties);
                    }
                )
                .fillBufferFn(MySlackSource::fillBufferFn)
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
            jsonObject.put(SlackConstants.PARAM_TEXT, cleanStr);

            return jsonObject;
        };
    }

    /**
     * <p>Create a sink that makes REST calls to write a JSON message to Slack's API.
     * </p>
     *
     * @param channel Used to name the job stage
     * @param properties To pass to the Sink builder
     * @param projectName To identify the sender
     * @return
     */
    private static Sink<JSONObject> mySlackChannelSink(String channel, Properties properties, String projectName) {
        return SinkBuilder.sinkBuilder(
                    "slackSink-" + channel,
                    context -> new MySlackSink(properties, projectName)
                )
                .receiveFn(
                        (MySlackSink mySlackSink, JSONObject item) -> mySlackSink.receiveFn(item)
                        )
                .destroyFn(mySlackSink -> mySlackSink.destroyFn())
                .preferredLocalParallelism(1)
                .build();
    }
}
