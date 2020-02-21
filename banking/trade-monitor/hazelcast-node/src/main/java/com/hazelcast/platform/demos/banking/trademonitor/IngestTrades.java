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

package com.hazelcast.platform.demos.banking.trademonitor;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

/**
 * <p>Creates a Jet pipeline to upload from a Kafka topic into a
 * Hazelcast map.
 */
public class IngestTrades {

    private static final long LOG_THRESHOLD = 100_000L;

    /**
     * <p>A simple ingest pipeline.
     * </p>
     * <p>The "{@code readFrom()}" source stage needs some properties to control
     * Kafka connectivity, an optional projection function to reformat the record
     * read into something more suitable, and at least one topic name to read from.
     * </p>
     * <p>The "{@code writeTo()}" sink stage writes into a {@link com.hazelcast.map.IMap}.
     * </p>
     * <p>On the Kafka topic the value is described as a string, but it's actually
     * JSON so the projection function converts it to JSON so that fast searching on
     * the JSON attributes is possible.
     * </p>
     *
     * @param bootstrapServers Kafka brokers list
     * @return A pipeline to run
     */
    public static Pipeline buildPipeline(String bootstrapServers) {

        Properties properties = IngestTrades.kafkaSourceProperties(bootstrapServers);

        Pipeline pipeline = Pipeline.create();

        StreamStage<Entry<String, HazelcastJsonValue>> inputSource =
            pipeline.readFrom(KafkaSources.<String, String, Entry<String, HazelcastJsonValue>>
                kafka(properties,
                record -> Util.entry(record.key(), new HazelcastJsonValue(record.value())),
                MyConstants.KAFKA_TOPIC_NAME_TRADES)
                )
         .withoutTimestamps();

        inputSource
         .writeTo(Sinks.map("trades"));

        /* To help with diagnostics, allow every 100,0000th item through
         * on each node. Nulls are filtered out.
         */
        inputSource
        .mapUsingService(ServiceFactories.sharedService(__ -> new LongAccumulator()),
            (counter, item) -> {
                counter.subtract(1);
                if (counter.get() <= 0) {
                    counter.set(LOG_THRESHOLD);
                    return item;
                }
                return null;
        }).setName("log_every_" + LOG_THRESHOLD)
        .writeTo(Sinks.logger());

        return pipeline;
    }


    /**
     * <p>Control properties for the Kafka connector.
     * </p>
     * <ul>
     * <li><p><i>AUTO_OFFSET_RESET_CONFIG</i> - where to begin reading from.
     * <p></li>
     * <li><p><i>BOOTSTRAP_SERVERS_CONFIG</i> - the list of brokers to connect to.
     * <p></li>
     * <li><p><i>GROUP_ID_CONFIG</i> - The Id for the Jet job connecting to Kafka,
     * make it unique rather than rely on Kafka generating one.
     * <p></li>
     * <li><p><i>KEY_DESERIALIZER_CLASS_CONFIG</i> - how to de-serialize the message key.
     * <p></li>
     * <li><p><i>VALUE_DESERIALIZER_CLASS_CONFIG</i> - how to de-serialize the message value.
     * <p></li>
     * </ul>
     *
     * @param bootstrapServers A CSV list of brokers
     * @return Properties used by the Kafka connector
     */
    private static Properties kafkaSourceProperties(String bootstrapServers) {

        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());

        return properties;
    }

}
