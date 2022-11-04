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

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.function.BiFunctionEx;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.sql.SqlResult;

/**
 * <p>Creates a Jet job that listens to Slack for SQL input
 * and responds to Slack with SQL output.
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
 *                |   Slack Sink    |
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
public class UtilsSlackSQLJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilsSlackSQLJob.class);

    // Local constant, never needed outside this class
    private static final List<String> ALLOWED_PREFIXES = List.of("SELECT");

    /**
     * <p>Validates the properties and launches the job.
     * </p>
     *
     * @param hazelcastInstance
     * @param projectName
     * @throws Exception
     */
    public static void submitJob(HazelcastInstance hazelcastInstance, String projectName)
        throws Exception {
        if (projectName == null || projectName.length() == 0) {
            String message = String.format("%s: Project name property not set, check '%s'",
                    UtilsSlackSQLJob.class.getSimpleName(),
                    UtilsConstants.SLACK_PROJECT_NAME);
            throw new RuntimeException(message);
        }
        Properties slackProperties = UtilsSlack.loadSlackAccessProperties();
        String accessToken = safeGet(slackProperties, UtilsConstants.SLACK_ACCESS_TOKEN);
        String buildUser = safeGet(slackProperties, UtilsConstants.SLACK_BUILD_USER);
        String channelId = safeGet(slackProperties, UtilsConstants.SLACK_CHANNEL_ID);
        String channelName = safeGet(slackProperties, UtilsConstants.SLACK_CHANNEL_NAME);
        if (accessToken.length() == 0 || channelId.length() == 0 || channelName.length() == 0) {
            LOGGER.warn("{}: missing values for '{}', '{}' and/or '{}',"
                    + " not launching Slack SQL integration",
                    UtilsSlackSQLJob.class.getSimpleName(),
                    UtilsConstants.SLACK_ACCESS_TOKEN,
                    UtilsConstants.SLACK_CHANNEL_ID,
                    UtilsConstants.SLACK_CHANNEL_NAME);
            return;
        }

        String jobName = UtilsSlackSQLJob.class.getSimpleName().replace("Utils", "");

        Pipeline pipelineUtilsSlackSQLJob = UtilsSlackSQLJob.buildPipeline(
                accessToken, channelId, channelName, projectName, buildUser);

        JobConfig jobConfigUtilsSlackSQLJob = new JobConfig();
        jobConfigUtilsSlackSQLJob.setName(jobName);
        jobConfigUtilsSlackSQLJob.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
        jobConfigUtilsSlackSQLJob.addClass(UtilsSlackSQLJob.class);
        jobConfigUtilsSlackSQLJob.addClass(UtilsSlackSource.class);
        jobConfigUtilsSlackSQLJob.addClass(UtilsSlackSink.class);

        try {
            Job job =
                    UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineUtilsSlackSQLJob, jobConfigUtilsSlackSQLJob);
            if (job == null) {
                String message = String.format("%s:submitJob: job '%s' not launched, exists already",
                        UtilsSlackSQLJob.class.getSimpleName(),
                        jobConfigUtilsSlackSQLJob.getName()
                        );
                LOGGER.info(message);
            } else {
                String message = String.format("%s:submitJob: job '%s' launched, status %s, id %d",
                        UtilsSlackSQLJob.class.getSimpleName(),
                        job.getName(),
                        job.getStatus(),
                        job.getId()
                        );
                LOGGER.info(message);
            }
        } catch (Exception e) {
            String message = String.format("%s:submitJob",
                    UtilsSlackSQLJob.class.getSimpleName()
                    );
            LOGGER.error(message, e);
        }
    }

    /**
     * <p>Null safe getter
     * </p>
     *
     * @param properties
     * @param key
     * @return Not null
     */
    private static String safeGet(Properties properties, String key) {
        Object value = properties.get(key);
        if (value == null) {
            String message = String.format("%s:safeGet key '{}' is null",
                    UtilsSlackSQLJob.class.getSimpleName(), key
                    );
            LOGGER.error(message);
            return "";
        } else {
            String result = value.toString();
            if (result.length() == 0) {
                String message = String.format("%s:safeGet key '{}' is empty string",
                        UtilsSlackSQLJob.class.getSimpleName(), key
                        );
                LOGGER.error(message);
                return "";
            }
            if (result.startsWith("@")) {
                String message = String.format("%s:safeGet key '{}' begins '@', check"
                        + " for Maven property replacement from ~/.m2/settings.xml",
                        UtilsSlackSQLJob.class.getSimpleName(), key
                        );
                LOGGER.error(message);
                return "";
            }
            return result;
        }
    }

    /**
     * <p>Create the Slack pipeline, the same as the diagram
     * at the top of the page.
     * </p>
     *
     * @param accessToken To connect
     * @param channelId Where to send
     * @param channelName Where to send
     * @param projectName Prefix for message to help identify response source
     * @return
     */
    private static Pipeline buildPipeline(String accessToken, String channelId,
            String channelName, String projectName, String buildUser) {
        ServiceFactory<?, HazelcastInstance> hazelcastInstanceService =
                ServiceFactories.sharedService(context -> context.hazelcastInstance());

        Pipeline pipeline = Pipeline.create();

        // Step (1) from diagram
        StreamStage<String> streamSource =
                pipeline
                .readFrom(UtilsSlackSource.slackSource(accessToken, channelId, channelName))
                .withoutTimestamps();

        // Step (2) from diagram
        StreamStage<Tuple2<Boolean, String>> possibleSqlStatement =
                streamSource
                .map(str -> {
                    String[] tokens = str.split(" ");
                    return Tuple2.tuple2(ALLOWED_PREFIXES.contains(tokens[0]), str);
                })
                .setName("determine-if-handled");

        // Step (3) from diagram
        StreamStage<String> unhandledInput =
                possibleSqlStatement
                .filter(tuple2 -> !tuple2.f0())
                .map(Tuple2::f1)
                .map(str -> {
                    return "Sorry, only '"
                            + ALLOWED_PREFIXES
                            + "' commands handled, not '" + str + "'";
                })
                .setName("not-sql-statement");

        // Step (4) from diagram
        StreamStage<String> handledInput =
                possibleSqlStatement
                .filter(tuple2 -> tuple2.f0())
                .map(Tuple2::f1)
                .mapUsingServiceAsync(hazelcastInstanceService, mapAsyncSqlFn())
                .setLocalParallelism(1)
                .setName("is-sql-statement");

        // Step (5) from diagram
        StreamStage<JSONObject> jsonOutput =
                unhandledInput.merge(handledInput)
                .map(UtilsSlackSQLJob.myMapStage()).setName("reformat-to-JSON");

        // Step (6) from diagram
        jsonOutput
        .writeTo(UtilsSlackSink.slackSink(accessToken, channelName, projectName, buildUser));

        return pipeline;
    }

    /**
     * <p>Takes the current Hazelcast instance and what we expect to be a query
     * to execute. Return a multi-line output, formatted for Slack.
     * </p>
     *
     * @return
     */
    private static BiFunctionEx<? super HazelcastInstance, ? super String, ? extends CompletableFuture<String>>
        mapAsyncSqlFn() {
            return (hazelcastInstance, sql) -> {
                return CompletableFuture.supplyAsync(new Supplier<String>() {
                    @Override
                    public String get() {
                        LOGGER.debug("Query.....: '{}'", sql);
                        String query = UtilsFormatter.makeUTF8(sql);

                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("```");
                        stringBuilder.append("Query: ").append(query).append(UtilsConstants.NEWLINE);
                        try {
                            SqlResult sqlResult = hazelcastInstance.getSql().execute(query);
                            // F0 error, F1 warning, F2 result
                            Tuple3<String, String, List<String>> result =
                                    UtilsFormatter.prettyPrintSqlResult(sqlResult);
                            if (result.f0().length() > 0) {
                                stringBuilder.append(result.f0());
                            } else {
                                result.f2().stream().forEach(row -> stringBuilder.append(row +  UtilsConstants.NEWLINE));
                                stringBuilder.append(result.f1());
                            }
                        } catch (Exception e) {
                            // Info log, Hazelcast hasn't failed
                            LOGGER.info("{}:mapAsyncSqlFn '{}' for '{}'",
                                    UtilsSlackSQLJob.class.getSimpleName(), e.getMessage(), sql);
                            stringBuilder.append("FAILED: ").append(e.getMessage()).append(UtilsConstants.NEWLINE);
                        }
                        stringBuilder.append("```");
                        return UtilsFormatter.makeUTF8(stringBuilder.toString());
                    }
                });
            };
    }

    /**
     * <p>Turn input string into JSON object to send to Slack.
     * </p>
     *
     * @return
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
