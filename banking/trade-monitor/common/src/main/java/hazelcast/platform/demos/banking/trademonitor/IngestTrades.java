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

package hazelcast.platform.demos.banking.trademonitor;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.function.SupplierEx;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.contrib.pulsar.PulsarSources;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.utils.UtilsUrls;

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
    public static Pipeline buildPipeline(String bootstrapServers, String pulsarList, boolean usePulsar) {

        Properties properties = InitializerConfig.kafkaSourceProperties(bootstrapServers);

        Pipeline pipeline = Pipeline.create();

        StreamStage<Entry<String, HazelcastJsonValue>> inputSource;
        if (usePulsar) {
            inputSource =
                    pipeline.readFrom(IngestTrades.pulsarSource(pulsarList))
                    .withoutTimestamps();
        } else {
            inputSource =
                    pipeline.readFrom(KafkaSources.<String, String, Entry<String, HazelcastJsonValue>>
                        kafka(properties,
                        record -> Util.entry(record.key(), new HazelcastJsonValue(record.value())),
                        MyConstants.KAFKA_TOPIC_NAME_TRADES)
                        )
                 .withoutTimestamps();
        }

        inputSource
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_TRADES));

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
        }).setName("filter_every_" + LOG_THRESHOLD)
        .writeTo(Sinks.logger());

        return pipeline;
    }

    /**
     * <p>This is similar to {@link AggregateQuery#IngestTrades()} but
     * returns a different type.
     * </p>
     * @param pulsarList
     * @return
     */
    private static StreamSource<Entry<String, HazelcastJsonValue>> pulsarSource(String pulsarList) {
        String serviceUrl = UtilsUrls.getPulsarServiceUrl(pulsarList);

        SupplierEx<PulsarClient> pulsarConnectionSupplier =
                () -> PulsarClient.builder()
                .connectionTimeout(1, TimeUnit.SECONDS)
                .serviceUrl(serviceUrl)
                .build();

        SupplierEx<Schema<String>> pulsarSchemaSupplier =
                () -> Schema.STRING;

        FunctionEx<Message<String>, Entry<String, HazelcastJsonValue>> pulsarProjectionFunction =
                message -> {
                    String key = message.getKey();
                    String trade = message.getValue();
                    HazelcastJsonValue value = new HazelcastJsonValue(trade);
                    return new SimpleImmutableEntry<>(key, value);
                };

        return PulsarSources.pulsarReaderBuilder(
            MyConstants.PULSAR_TOPIC_NAME_TRADES,
            pulsarConnectionSupplier,
            pulsarSchemaSupplier,
            pulsarProjectionFunction).build();
    }

}
