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
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerMetadata;

/**
 * <p>Take a "{@code <String, String>}" stream from Kafka, convert
 * to "{@code <String, HazelcastJsonValue>}", keep those
 * that didn't originate from Hazelcast, and save to Hazelcast.
 * </p>
 * <p>Similar to {@link CassandraDebeziumTwoWayCDC}, different topic
 * input, different map output, different reformmating, but
 * the same idea overall.
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
 * Mongo source
 * </p>
 * <p>Debezium sends changes made to Mongo's "{@code customer}" collection
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
public class MongoDebeziumTwoWayCDC extends MyJobWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDebeziumTwoWayCDC.class);

    private String myMongo;
    private String bootstrapServers;

    MongoDebeziumTwoWayCDC(long timestamp, String bootstrapServers) {
        super(timestamp);
        this.bootstrapServers = bootstrapServers;

        // Configure expected MySql address for Docker or Kubernetes
        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            this.myMongo =
                 System.getProperty("my.project") + "-mongo.default.svc.cluster.local";

            LOGGER.info("Kubernetes configuration: mongo host: '{}'", this.myMongo);
        } else {
            this.myMongo = "mongo";
            LOGGER.info("Non-Kubernetes configuration: mongo host: '{}'", this.myMongo);
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
                kafkaConnectionProperties, MyConstants.KAFKA_TOPIC_MONGO)).withoutTimestamps()
        .map(MongoDebeziumTwoWayCDC::filterConvert)
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CUSTOMER));

        return pipeline;
    }

    /**
     * <p>Connection properties for Kafka, key and value are passed
     * as strings by Debezium for Mongo.
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
     *
     * @param entry From Kafka
     * @return Null if the entry is filtered out
     */
    private static Entry<String, HazelcastJsonValue> filterConvert(Entry<String, String> entry) {
        String oldValue = entry.getValue();
        try {
            JSONObject json = new JSONObject(oldValue);

            // Discard the metadata, we only need the post-update record
            JSONObject payload = json.getJSONObject("payload");

            // Is this a create (after) or an update (patch) ?
            JSONObject create = fetch(payload, "after");
            JSONObject update = fetch(payload, "patch");
            if (create == null && update == null) {
                LOGGER.error("Payload has neither 'after' *AND* 'patch' for key '{}'", entry.getKey());
                return null;
            }
            if (create != null && update != null) {
                LOGGER.error("Payload has 'after' *AND* 'patch' for key '{}', assume patch", entry.getKey());
            }
            JSONObject cdc = (update == null ? create : update);
            String description = (update == null ? "CREATE" : "UPDATE");

            // Strings are as-is, others are nested as JSON
            String id = cdc.getString("_" + CustomerMetadata.ID);
            String firstName = cdc.getString(CustomerMetadata.FIRSTNAME);
            String lastName = cdc.getString(CustomerMetadata.LASTNAME);
            String accountType = cdc.getString(CustomerMetadata.ACCOUNT_TYPE);
            String createdBy = cdc.getString(CustomerMetadata.CREATED_BY);
            JSONObject createdDateJson = cdc.getJSONObject(CustomerMetadata.CREATED_DATE);
            String lastModifiedBy = cdc.getString(CustomerMetadata.LAST_MODIFIED_BY);
            JSONObject lastModifiedDateJson = cdc.getJSONObject(CustomerMetadata.LAST_MODIFIED_DATE);

            JSONArray notesArray = cdc.getJSONArray(CustomerMetadata.NOTES);
            String[] notes = new String[notesArray.length()];
            for (int i = 0; i < notes.length; i++) {
                notes[i] = notesArray.get(i).toString();
            }

            // Only accept changes from disk by `churn-update-legacy`
            if (!lastModifiedBy.contains("update")) {
                LOGGER.trace("Exclude {} made by '{}'", description, lastModifiedBy);
                return null;
            } else {
                LOGGER.debug("Include {} made by '{}' to Id '{}'",
                        description, lastModifiedBy, id);
            }

            // Values that are nested JSON, unpick if got this far
            Long createdDate = Long.valueOf(createdDateJson.getString("$numberLong"));
            Long lastModifiedDate = Long.valueOf(lastModifiedDateJson.getString("$numberLong"));

            // Extract the new value
            HazelcastJsonValue newValue = formNewValue(id, firstName, lastName,
                    accountType, createdBy, createdDate,
                    lastModifiedBy, lastModifiedDate, notes);

            return Tuple2.tuple2(id, newValue);
        } catch (NumberFormatException numberFormatException) {
            LOGGER.error("filterConvert('{}'), NUMBER FORMAT EXCEPTION: {}",
                    oldValue, numberFormatException.getMessage());
        } catch (Exception exception) {
            LOGGER.error("filterConvert('{}'), EXCEPTION: {}", oldValue, exception.getMessage());
        }

        return null;
    }

    /**
     * <p>Fetch a field that may be missing or null.
     * </p>
     *
     * @param payload
     * @param fieldName
     * @return Null if no data
     */
    private static JSONObject fetch(JSONObject payload, String fieldName) {
        if (payload.has(fieldName) && !payload.isNull(fieldName)) {
            return new JSONObject(payload.getString(fieldName));
        } else {
            return null;
        }
    }


    /**
     * <p>Reformatting.
     * </p>
     *
     * @param id
     * @param firstName
     * @param lastName
     * @param accountType
     * @param createdBy
     * @param createdDate
     * @param lastModifiedBy
     * @param lastModifiedDate
     * @param notes
     * @return
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private static HazelcastJsonValue formNewValue(String id, String firstName,
            String lastName, String accountType, String createdBy, long createdDate,
            String lastModifiedBy, long lastModifiedDate, String[] notes) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("  \"id\" : \"" + id + "\"");
        stringBuilder.append(", \"firstName\" : \"" + firstName + "\"");
        stringBuilder.append(", \"lastName\" : \"" + lastName + "\"");
        stringBuilder.append(", \"accountType\" : \"" + accountType + "\"");
        stringBuilder.append(", \"createdBy\" : \"" + createdBy + "\"");
        stringBuilder.append(", \"createdDate\" : " + createdDate);
        stringBuilder.append(", \"lastModifiedBy\" : \"" + lastModifiedBy + "\"");
        stringBuilder.append(", \"lastModifiedDate\" : " + lastModifiedDate);
        stringBuilder.append(", \"notes\" : [");
        for (int i = 0; i < notes.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append("\"" + notes[i] + "\"");
        }
        stringBuilder.append("]");
        stringBuilder.append(" }");

        return new HazelcastJsonValue(stringBuilder.toString());
    }

}
