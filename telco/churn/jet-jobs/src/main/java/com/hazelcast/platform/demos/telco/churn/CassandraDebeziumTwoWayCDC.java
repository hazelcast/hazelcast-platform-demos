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

import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordKey;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordMetadata;

/**
 * <p>Take a "{@code <String, String>}" stream from Kafka, convert
 * to "{@code <CallDataRecordKey, HazelcastJsonValue>}", keep those
 * that didn't originate from Hazelcast, and save to Hazelcast.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                |Cassandra Source |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                |Reformat & Filter|
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 3 )------+
 *                |    IMap Sink    |
 *                +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * Cassandra source
 * </p>
 * <p>Debezium sends changes made to Cassandra's "{@code cdr}" table
 * to a Kafka topic.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformat & filter
 * </p>
 * <p>Turn the CDC record into a "{@code Map<String, HazelcastJsonValue}"
 * record. Filter out (by returning "{@code null}") and that didn't
 * originate external to Hazelcast.
 * </p>
 * </li>
 * <li>
 * <p>
 * Map sink
 * </p>
 * <p>Save everything read into a map.
 * </p>
 * </li>
 * </ol>
 */
public class CassandraDebeziumTwoWayCDC extends MyJobWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraDebeziumTwoWayCDC.class);

    private String myCassandra;
    private String bootstrapServers;

    CassandraDebeziumTwoWayCDC(long timestamp, String bootstrapServers) {
        super(timestamp);
        this.bootstrapServers = bootstrapServers;

        // Configure expected Cassandra address for Docker or Kubernetes
        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            this.myCassandra =
                 System.getProperty("my.project") + "-cassandra.default.svc.cluster.local";

            LOGGER.info("Kubernetes configuration: cassandra host: '{}'", this.myCassandra);
        } else {
            this.myCassandra = "cassandra";
            LOGGER.info("Non-Kubernetes configuration: cassandra host: '{}'", this.myCassandra);
        }
    }

    /**
     * <p>Create the pipeline.
     * </p>
     */
    public Pipeline getPipeline() {
        Properties kafkaConnectionProperties = buildKafkaConnectionProperties(this.bootstrapServers);

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(KafkaSources.<String, String>kafka(
                kafkaConnectionProperties, MyConstants.KAFKA_TOPIC_CASSANDRA)).withoutTimestamps()
        .map(CassandraDebeziumTwoWayCDC::filterConvert)
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CDR));

        return pipeline;
    }

    /**
     * <p>Connection properties for Kafka, key and value are passed
     * as strings by Debezium for Cassandra.
     * </p>
     */
    private static Properties buildKafkaConnectionProperties(String bootstrapServers) {
        Properties kafkaProperties = new Properties();

        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        kafkaProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());

        return kafkaProperties;
    }

    /**
     * <p>Convert the entry into the required type for storage in Hazelcast.
     * Discard any entries with the incorrect modifier flag, so we don't
     * get into a loop of uploading changes we created.
     * </p>
     * TODO: The mapping between JSON field names (eg. "{@code caller_telno}")
     * and Java counterparts ("{@code callerTelno}") needs handled better
     * across the application as a whole.
     *
     * @param entry From Kafka
     * @return Null if the entry is filtered out
     */
    private static Entry<CallDataRecordKey, HazelcastJsonValue> filterConvert(Entry<String, String> entry) {
        String oldValue = entry.getValue();
        try {
            JSONObject json = new JSONObject(oldValue);
            // Discard the metadata, we only need the post-update record
            json = json.getJSONObject("after");

            // Each field is presented as JSON
            JSONObject idJson = json.getJSONObject(CallDataRecordMetadata.ID);
            JSONObject callSuccessfulJson = json.getJSONObject("call_successful");
            JSONObject calleeMastIdJson = json.getJSONObject("callee_mast_id");
            JSONObject calleeTelnoJson = json.getJSONObject("callee_telno");
            JSONObject callerMastIdJson = json.getJSONObject("caller_mast_id");
            JSONObject callerTelnoJson = json.getJSONObject("caller_telno");
            JSONObject createdByJson = json.getJSONObject("created_by");
            JSONObject createdDateJson = json.getJSONObject("created_date");
            JSONObject durationSecondsJson = json.getJSONObject("duration_seconds");
            JSONObject lastModifiedByJson = json.getJSONObject("last_modified_by");
            JSONObject lastModifiedDateJson = json.getJSONObject("last_modified_date");
            JSONObject startTimestampJson = json.getJSONObject("start_timestamp");

            String lastModifiedBy = lastModifiedByJson.getString("value");
            // Only accept changes from disk by `churn-update-legacy`
            if (!lastModifiedBy.contains("update")) {
                LOGGER.trace("Exclude change made by '{}'", lastModifiedBy);
                return null;
            } else {
                LOGGER.debug("Include change made by '{}' to Id '{}'",
                        lastModifiedBy, idJson.getString("value"));
            }

            // Extract the new value
            String id = idJson.getString("value");
            boolean callSuccessful = callSuccessfulJson.getBoolean("value");
            String calleeMastId = calleeMastIdJson.getString("value");
            String calleeTelno = calleeTelnoJson.getString("value");
            String callerMastId = callerMastIdJson.getString("value");
            String callerTelno = callerTelnoJson.getString("value");
            String createdBy = createdByJson.getString("value");
            long createdDate = createdDateJson.getLong("value");
            int durationSeconds = durationSecondsJson.getInt("value");
            long lastModifiedDate = lastModifiedDateJson.getLong("value");
            long startTimestamp = startTimestampJson.getLong("value");

            CallDataRecordKey newKey = new CallDataRecordKey();
            newKey.setCsv(callerTelno + "," + id);

            HazelcastJsonValue newValue = formNewValue(id, callSuccessful,
                    calleeMastId, calleeTelno, callerMastId, callerTelno,
                    createdBy, createdDate, durationSeconds,
                    lastModifiedBy, lastModifiedDate, startTimestamp);

            return Tuple2.tuple2(newKey, newValue);

        } catch (Exception exception) {
            LOGGER.error("filterConvert('{}'), EXCEPTION: {}", oldValue, exception.getMessage());
        }

        return null;
    }

    /**
     * <p>Reformatting.
     * </p>
     *
     * @param id
     * @param callSuccessful
     * @param calleeMastId
     * @param calleeTelno
     * @param callerMastId
     * @param callerTelno
     * @param createdBy
     * @param createdDate
     * @param durationSeconds
     * @param lastModifiedBy
     * @param lastModifiedDate
     * @param startTimestamp
     * @return
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private static HazelcastJsonValue formNewValue(String id, boolean callSuccessful, String calleeMastId,
            String calleeTelno, String callerMastId, String callerTelno, String createdBy, long createdDate,
            int durationSeconds, String lastModifiedBy, long lastModifiedDate, long startTimestamp) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("  \"id\" : \"" + id + "\"");
        stringBuilder.append(", \"callSuccessful\" : " + callSuccessful);
        stringBuilder.append(", \"calleeMastId\" : \"" + calleeMastId + "\"");
        stringBuilder.append(", \"calleeTelno\" : \"" + calleeTelno + "\"");
        stringBuilder.append(", \"callerMastId\" : \"" + callerMastId + "\"");
        stringBuilder.append(", \"callerTelno\" : \"" + callerTelno + "\"");
        stringBuilder.append(", \"createdBy\" : \"" + createdBy + "\"");
        stringBuilder.append(", \"createdDate\" : " + createdDate);
        stringBuilder.append(", \"durationSeconds\" : " + durationSeconds);
        stringBuilder.append(", \"lastModifiedBy\" : \"" + lastModifiedBy + "\"");
        stringBuilder.append(", \"lastModifiedDate\" : " + lastModifiedDate);
        stringBuilder.append(", \"startTimestamp\" : " + startTimestamp);
        stringBuilder.append(" }");

        return new HazelcastJsonValue(stringBuilder.toString());
    }

}
