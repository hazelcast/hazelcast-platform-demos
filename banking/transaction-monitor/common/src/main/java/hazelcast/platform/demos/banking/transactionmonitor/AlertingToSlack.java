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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.utils.UtilsConstants;
import com.hazelcast.platform.demos.utils.UtilsSlackSink;

/**
 * <p>A simple job that listens on a map and republishes to Slack.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                | Journal Source  |
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
 * Map journal source
 * </p>
 * <p>Stream changes from the "{@code alertMax}" map.
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
 * <p>Write input back to the same Slack channel as used for SQL in/out.
 * </p>
 * </li>
 * </ol>
 */
public class AlertingToSlack {

    /**
     * <p>A republishing pipeline.
     * </p>
     *
     * @param accessTokenObject String from properties
     * @param channelNameObject String from properties
     * @param projectNameObject String from properties
     * @param buildUsser String from properties
     */
    public static Pipeline buildPipeline(Object accessTokenObject,
            Object channelNameObject, Object projectNameObject, Object buildUserObject,
            TransactionMonitorFlavor transactionMonitorFlavor) throws Exception {
        String accessToken = validate("accessToken", accessTokenObject);
        String channelName = validate("channelName", channelNameObject);
        String projectName = validate("projectName", projectNameObject);
        String buildUser = validate("buildUser", buildUserObject);

        Pipeline pipeline = Pipeline.create();

        StreamStage<JSONObject> input = pipeline
        .readFrom(Sources.<Long, HazelcastJsonValue>mapJournal(
                MyConstants.IMAP_NAME_ALERTS_LOG,
                JournalInitialPosition.START_FROM_OLDEST)).withoutTimestamps()
        .map(AlertingToSlack.myMapStage(transactionMonitorFlavor)).setName("reformat-to-JSON");

        input
        .writeTo(UtilsSlackSink.slackSink(accessToken, channelName, projectName, buildUser));

        input
        .writeTo(Sinks.logger());

        return pipeline;
    }

    /**
     * <p>Check property set
     * </p>
     *
     * @param key Property name
     * @param value From "properties.get(K)", will be a String or null.
     * @return
     * @throws Exception
     */
    private static String validate(String key, Object value) throws Exception {
        String result = (value == null ? "" : value.toString());
        if (result.length() == 0) {
            throw new RuntimeException("No value for '" + key + "'");
        }
        return result;
    }

    /**
     * <p>Extract the JSON from the map entry.
     * </p>
     */
    private static FunctionEx<Map.Entry<Long, HazelcastJsonValue>, JSONObject> myMapStage(
            TransactionMonitorFlavor transactionMonitorFlavor) {
        return entry -> {
            JSONObject output = new JSONObject();

            try {
                JSONObject input = new JSONObject(entry.getValue().toString());

                String fieldName1;
                String fieldValue1;
                String fieldName2;
                String fieldValue2;
                switch (transactionMonitorFlavor) {
                case ECOMMERCE:
                    fieldName1 = "code";
                    fieldValue1 = safeGetString(input, "code");
                    fieldName2 = "sales";
                    fieldValue2 = safeGetDouble(input, "volume");
                    break;
                case PAYMENTS_ISO20022:
                    fieldName1 = "bic";
                    fieldValue1 = safeGetString(input, "code");
                    fieldName2 = "volume";
                    fieldValue2 = safeGetDouble(input, "volume");
                    break;
                case TRADE:
                default:
                    fieldName1 = "stock";
                    fieldValue1 = safeGetString(input, "code");
                    fieldName2 = "volume";
                    fieldValue2 = safeGetDouble(input, "volume");
                    break;
                }

                String cleanStr =
                        "*ALERT* `"
                        + safeGetString(input, "whence")
                        + ", " + fieldName1 + ": '" + fieldValue1 + "'"
                        + ", " + fieldName2 + ": " + fieldValue2 + ""
                        + ", provenance: '" + safeGetString(input, "provenance") + "'`";

                output.put(UtilsConstants.SLACK_PARAM_TEXT, cleanStr);
                return output;
            } catch (Exception e) {
                System.out.println(Objects.toString(entry) + " caused " + e.getMessage());
                output.put(UtilsConstants.SLACK_PARAM_TEXT, "no data");
                return output;
            }
        };
    }

    /**
     * <p>Defensive access to JSON
     * </p>
     *
     * @param input
     * @param field
     * @return
     */
    private static String safeGetString(JSONObject input, String field) {
        try {
            return Objects.toString(input.getString(field));
        } catch (Exception e) {
            return String.format("Exception for %s:%s in '%s'", field, e.getMessage(), input);
        }
    }

    /**
     * <p>Defensive access to JSON
     * </p>
     *
     * @param input
     * @param field
     * @return
     */
    private static String safeGetDouble(JSONObject input, String field) {
        try {
            Double d = input.getDouble(field);
            return String.format("%-2f", d);
        } catch (Exception e) {
            return String.format("Exception for %s:%s", field, e.getMessage());
        }
    }
}
