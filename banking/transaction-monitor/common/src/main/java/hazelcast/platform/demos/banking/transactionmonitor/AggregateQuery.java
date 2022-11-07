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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.function.Functions;
import com.hazelcast.function.SupplierEx;
import com.hazelcast.function.ToLongFunctionEx;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.accumulator.MutableReference;
import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.contrib.pulsar.PulsarSources;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.platform.demos.utils.UtilsUrls;

/**
 * <p>Creates a pipeline job to "<i>query</i>" Kafka transactions.
 * </p>
 * <p>Really what this does is a <b><i>continuous</i></b> aggregation.
 * </p>
 * <p>All transactions are read from Kafka. We treat the Kafka stream as
 * continuous. Although the stock market has opening and closing hours,
 * other markets such as crypto-currencies are 24 hours a day.
 * </p>
 * <p>With a continuous input, we produce a <i>rolling aggregate</i>,
 * the running total since the start. We could if we had wanted do
 * a "<i>last 5 minutes</i>" approach, lots of ways exist.
 * </p>
 * <p>So the essence here is to read from Kafka, just like
 * {@link IngestTransactions} and to write the answer into an
 * {@link com.hazelcast.map.IMap} just like {@link IngestTransactions}.
 * What is different is that in-between we summarise by 3 factors,
 * the simple count, the volume ({@code price * quantity)}, and
 * the latest price.
 * </p>
 */
public class AggregateQuery {

    private static final int CONSTANT_KEY = Integer.valueOf(0);
    private static final long FIVE_MINUTES_IN_MS = 5 * 60 * 1_000L;
    private static final long LOG_THRESHOLD = 100_000L;

    private static ToLongFunctionEx<Object> nowTimestampFn = __ -> System.currentTimeMillis();

    /**
     * <p>Read from Kafka, aggregate, store in a map.
     * </p>
     * <p>We read a String that happens to be JSON from Kafka.
     * As this time we want to get at the JSON values, change
     * the de-serializer call so we can do so.
     * </p>
     * <p>This different de-serialiser is required only until
     * <a href="https://github.com/hazelcast/hazelcast/issues/15140">Issue-15150</a>
     * is addressed.
     * </p>
     *
     * @param bootstrapServers Connection list for Kafka
     * @return A pipeline job to run in Jet.
     */
    public static Pipeline buildPipeline(String bootstrapServers, String pulsarList,
            boolean usePulsar, String projectName, String jobName, String clusterName) {

        // Override the value de-serializer to produce a different type
        Properties properties = InitializerConfig.kafkaSourceProperties(bootstrapServers);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, TransactionJsonDeserializer.class.getName());

        Pipeline pipeline = Pipeline.create();

        StreamStage<Transaction> inputSource;
        if (usePulsar) {
            inputSource =
                    pipeline.readFrom(AggregateQuery.pulsarSource(pulsarList))
                    .withoutTimestamps();
        } else {
            inputSource =
                pipeline.readFrom(KafkaSources.<String, Transaction, Transaction>
                    kafka(properties,
                    ConsumerRecord::value,
                    MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS)
                    )
            .withoutTimestamps();
        }

        StreamStage<Entry<String, Tuple3<Long, Long, Long>>> aggregated =
            inputSource
            .mapUsingIMap(MyConstants.IMAP_NAME_SYMBOLS,
                    Transaction::getSymbol,
                    (Transaction transaction, SymbolInfo symbolInfo)
                    -> (symbolInfo.getFinancialStatus() == NasdaqFinancialStatus.NORMAL ? transaction : null))
            .groupingKey(Transaction::getSymbol)
            .rollingAggregate(AggregateOperations.allOf(
                AggregateOperations.counting(),
                AggregateOperations.summingLong(transaction -> transaction.getPrice() * transaction.getQuantity()),
                latestValue(transaction -> Long.valueOf(transaction.getPrice()))
               ))
               .setName("aggregate by symbol");

        aggregated
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS));

        /* To help with diagnostics, allow every 100,0000th item through
         * on each node. Nulls are filtered out.
         */
        aggregated
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

        // Extra stages for alert generation
        AggregateQuery.addMaxVolumeAlert(aggregated, projectName, clusterName, jobName);

        return pipeline;
    }


    /**
     * <p>A custom aggregator to track the last value, stashing it in
     * a reference object, replacing the previous. Because execution
     * is in parallel values may be received from two sources at once
     * giving a minor race condition that does not matter here.
     * </p>
     *
     * @param getLongValueFn How to get an "{@code long}" from the transaction
     * @return A long
     */
    private static AggregateOperation1<Transaction, MutableReference<Long>, Long> latestValue(
            ToLongFunctionEx<Transaction> getLongValueFn) {
        return AggregateOperation
            .withCreate(() -> new MutableReference<Long>())
            .andAccumulate((MutableReference<Long> reference, Transaction transaction) -> {
                reference.set(getLongValueFn.applyAsLong(transaction));
            })
            .andExportFinish(MutableReference::get);
    }

    /**
     * <p>Periodically (every <i>n</i> minutes) output the largest
     * trading stock by volume.
     * </p>
     * <p>This is the largest since the start, not in that
     * time period.
     * </p>
     *
     * @param aggregated
     */
    private static void addMaxVolumeAlert(
            StreamStage<Entry<String, Tuple3<Long, Long, Long>>> aggregated,
            String projectName, String jobName, String clusterName) {
        AggregateOperation1<
            Entry<Integer, Tuple4<String, Long, Long, Long>>,
            MaxVolumeAggregator,
            Entry<Long, HazelcastJsonValue>>
                maxVolumeAggregator =
                    MaxVolumeAggregator.buildMaxVolumeAggregation(projectName, clusterName, jobName);

        aggregated
        .map(entry -> {
            Tuple4<String, Long, Long, Long> tuple4 =
                    Tuple4.tuple4(entry.getKey(), entry.getValue().f0(),
                            entry.getValue().f1(), entry.getValue().f2());
            return new SimpleImmutableEntry<>(CONSTANT_KEY, tuple4);
        })
        .addTimestamps(nowTimestampFn, 0)
        .groupingKey(Functions.entryKey())
        .window(WindowDefinition.tumbling(FIVE_MINUTES_IN_MS))
        .aggregate(maxVolumeAggregator)
        .map(result -> result.getValue())
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME));
    }

    /**
     * <p>This is similar to {@link IngestTransactions#IngestTransactions()} but
     * returns a different type.
     * </p>
     *
     * @param pulsarList
     * @return
     */
    private static StreamSource<Transaction> pulsarSource(String pulsarList) {
        String serviceUrl = UtilsUrls.getPulsarServiceUrl(pulsarList);

        SupplierEx<PulsarClient> pulsarConnectionSupplier =
                () -> PulsarClient.builder()
                .connectionTimeout(1, TimeUnit.SECONDS)
                .serviceUrl(serviceUrl)
                .build();

        SupplierEx<Schema<String>> pulsarSchemaSupplier =
                () -> Schema.STRING;

        FunctionEx<Message<String>, Transaction> pulsarProjectionFunction =
                message -> {
                    //TODO: A new deserializer for each message, could optimize with shared if thread-safe
                    try (TransactionJsonDeserializer transactionJsonDeserializer = new TransactionJsonDeserializer()) {
                        byte[] bytes = message.getValue().getBytes(StandardCharsets.UTF_8);
                        return transactionJsonDeserializer.deserialize("", bytes);
                    }
                };

        return PulsarSources.pulsarReaderBuilder(
                    MyConstants.PULSAR_TOPIC_NAME_TRANSACTIONS,
                    pulsarConnectionSupplier,
                    pulsarSchemaSupplier,
                    pulsarProjectionFunction).build();
    }
}
