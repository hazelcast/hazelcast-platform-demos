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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;

/**
 * FIXME Placeholder to add Debezium ingest for Mongo
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
        .readFrom(KafkaSources.<String, HazelcastJsonValue>kafka(
                kafkaConnectionProperties, MyConstants.KAFKA_TOPIC_MONGO)).withoutTimestamps()
        //FIXME Sink to Customer map
        .writeTo(Sinks.logger());

        return pipeline;
    }

    /**
     * <p>Connection properties for Kafka, custom deserializer
     * for value takes a String and converts to {@link HazelcastJsonValue}
     * FIXME Serializer
     * </p>
     */
    private static Properties buildKafkaConnectionProperties(String bootstrapServers) {
        Properties kafkaProperties = new Properties();

        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        kafkaProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        //FIXME Add to JobConfig if using
        kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MyKafkaValueDeserializer.class.getCanonicalName());
        kafkaProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());

        return kafkaProperties;
    }

}
