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
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;

/**
 * <p>A job to simply upload from Kafka into Hazelcast.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                |  Kafka Source   |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                |    IMap Sink    |
 *                +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * Kafka source
 * </p>
 * <p>Read all messages from the beginning from a Kafka topic.
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
public class KafkaIngest extends MyJobWrapper {

    private String bootstrapServers;

    KafkaIngest(long arg0, String arg1) {
        super(arg0);
        this.bootstrapServers = arg1;
    }

    /**
     * <p>Job configuration, mainly which classes need to be distributed
     * to execution nodes.
     * </p>
     */
    @Override
    JobConfig getJobConfig() {
        JobConfig jobConfig = super.getJobConfig();

        jobConfig.addClass(MyKafkaValueDeserializer.class);

        return jobConfig;
    }

    /**
     * <p>Read from Kafka, write to a map. No filter or other processing.
     */
    public Pipeline getPipeline() {
        Properties kafkaConnectionProperties = buildKafkaConnectionProperties(this.bootstrapServers);

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(KafkaSources.<String, HazelcastJsonValue>kafka(
                kafkaConnectionProperties, MyConstants.KAFKA_TOPIC_CALLS_NAME)).withoutTimestamps()
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CDR));

        return pipeline;
    }

    /**
     * <p>Connection properties for Kafka, custom deserializer
     * for value takes a String and converts to {@link HazelcastJsonValue}
     * </p>
     */
    private static Properties buildKafkaConnectionProperties(String bootstrapServers) {
        Properties kafkaProperties = new Properties();

        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        kafkaProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MyKafkaValueDeserializer.class.getCanonicalName());

        return kafkaProperties;
    }
}
