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
import com.hazelcast.function.ToDoubleFunctionEx;
import com.hazelcast.function.ToLongFunctionEx;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.accumulator.MutableReference;
import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.contrib.pulsar.PulsarSources;
import com.hazelcast.jet.datamodel.Tuple2;
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
            boolean usePulsar, String projectName, String jobName, String clusterName,
            TransactionMonitorFlavor transactionMonitorFlavor) {

        Properties properties = InitializerConfig.kafkaSourceProperties(bootstrapServers);

        Pipeline pipeline = Pipeline.create();

        StreamStage<Entry<String, Tuple3<Long, Double, Double>>> aggregated;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            aggregated = aggregatedEcommerce(pipeline, properties, usePulsar, pulsarList);
            break;
        case TRADE:
        default:
            aggregated = aggregatedTrade(pipeline, properties, usePulsar, pulsarList);
            break;
        }

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
        AggregateQuery.addMaxAlert(aggregated, projectName, clusterName, jobName);

        return pipeline;
    }

    /**
     * <p>E-commerce items are aggregated by item code, "{@code A0001}", etc.
     * Unlike trades, there is no filtering. Instead we compute the average
     * sale price.
     * </p>
     * <p>See also {@link PerspectiveEcommerce}.
     * </p>
     *
     * @return A trio for the product - count, sum and latest
     */
    private static StreamStage<Entry<String, Tuple3<Long, Double, Double>>> aggregatedEcommerce(
            Pipeline pipeline, Properties properties, boolean usePulsar, String pulsarList) {

        StreamStage<TransactionEcommerce> inputSource;
        if (usePulsar) {
            inputSource =
                    pipeline.readFrom(AggregateQuery.pulsarSourceEcommerce(pulsarList))
                    .withoutTimestamps();
        } else {
            // Override the value de-serializer to produce a different type
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    TransactionEcommerceJsonDeserializer.class.getName());

            inputSource =
                pipeline.readFrom(KafkaSources.<String, TransactionEcommerce, TransactionEcommerce>
                    kafka(properties,
                    ConsumerRecord::value,
                    MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS)
                    )
            .withoutTimestamps();
        }

        return inputSource
                .groupingKey(TransactionEcommerce::getItemCode)
                .rollingAggregate(AggregateOperations.allOf(
                    AggregateOperations.counting(),
                    AggregateOperations.summingDouble(transaction -> transaction.getPrice() * transaction.getQuantity())
                   ))
                .map((Entry<String, Tuple2<Long, Double>> entry) -> {
                    return (Entry<String, Tuple3<Long, Double, Double>>)
                            new SimpleImmutableEntry<>(entry.getKey(),
                            Tuple3.tuple3(entry.getValue().f0(),
                                    entry.getValue().f1(),
                                    Double.parseDouble(String.format("%.2f", entry.getValue().f1() / entry.getValue().f0()))));
                })
                .setName("aggregate by item code");
    }

    /**
     * <p>Trades are aggregated by stock symbol, "{@code AAL}", etc.
     * Stocks with specific status other than normal are filtered out,
     * we do not see "{@code DELINQUENT}" or "{@code DEFICIENT}".
     * </p>
     * <p>See also {@link PerspectiveTrade}.
     * </p>
     *
     * @return A trio for the stock - count, sum and latest
     */
    private static StreamStage<Entry<String, Tuple3<Long, Double, Double>>> aggregatedTrade(
            Pipeline pipeline, Properties properties, boolean usePulsar, String pulsarList) {

        StreamStage<TransactionTrade> inputSource;
        if (usePulsar) {
            inputSource =
                    pipeline.readFrom(AggregateQuery.pulsarSourceTrade(pulsarList))
                    .withoutTimestamps();
        } else {
            // Override the value de-serializer to produce a different type
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    TransactionTradeJsonDeserializer.class.getName());

            inputSource =
                pipeline.readFrom(KafkaSources.<String, TransactionTrade, TransactionTrade>
                    kafka(properties,
                    ConsumerRecord::value,
                    MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS)
                    )
            .withoutTimestamps();
        }

        return inputSource
                .mapUsingIMap(MyConstants.IMAP_NAME_SYMBOLS,
                        TransactionTrade::getSymbol,
                        (TransactionTrade transaction, SymbolInfo symbolInfo)
                        -> (symbolInfo.getFinancialStatus() == NasdaqFinancialStatus.NORMAL ? transaction : null))
                .groupingKey(TransactionTrade::getSymbol)
                .rollingAggregate(AggregateOperations.allOf(
                    AggregateOperations.counting(),
                    AggregateOperations.summingDouble(transaction -> transaction.getPrice() * transaction.getQuantity()),
                    latestTradeValue(transaction -> Double.valueOf(transaction.getPrice()))
                   ))
                   .setName("aggregate by symbol");
    }

    /**
     * <p>A custom aggregator to track the last value, stashing it in
     * a reference object, replacing the previous. Because execution
     * is in parallel values may be received from two sources at once
     * giving a minor race condition that does not matter here.
     * </p>
     *
     * @param getDoubleValueFn How to get a "{@code double}" from the transaction
     * @return A double
     */
    private static AggregateOperation1<TransactionTrade, MutableReference<Double>, Double>
        latestTradeValue(
            ToDoubleFunctionEx<TransactionTrade> getDoubleValueFn) {
        return AggregateOperation
            .withCreate(() -> new MutableReference<Double>())
            .andAccumulate((MutableReference<Double> reference, TransactionTrade transaction) -> {
                reference.set(getDoubleValueFn.applyAsDouble(transaction));
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
    private static void addMaxAlert(
            StreamStage<Entry<String, Tuple3<Long, Double, Double>>> aggregated,
            String projectName, String jobName, String clusterName) {
        AggregateOperation1<
            Entry<Integer, Tuple4<String, Long, Double, Double>>,
            MaxAggregator,
            Entry<Long, HazelcastJsonValue>>
                maxAggregator =
                    MaxAggregator.buildMaxAggregation(projectName, clusterName, jobName);

        aggregated
        .map(entry -> {
            Tuple4<String, Long, Double, Double> tuple4 =
                    Tuple4.tuple4(entry.getKey(), entry.getValue().f0(),
                            entry.getValue().f1(), entry.getValue().f2());
            return new SimpleImmutableEntry<>(CONSTANT_KEY, tuple4);
        })
        .addTimestamps(nowTimestampFn, 0)
        .groupingKey(Functions.entryKey())
        .window(WindowDefinition.tumbling(FIVE_MINUTES_IN_MS))
        .aggregate(maxAggregator)
        .map(result -> result.getValue())
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_ALERTS_LOG));
    }

    /**
     * <p>This is similar to {@link IngestTransactions#IngestTransactions()} but
     * returns a different type.
     * </p>
     *
     * @param pulsarList
     * @return
     */
    @SuppressWarnings("unchecked")
    private static StreamSource<TransactionEcommerce> pulsarSourceEcommerce(String pulsarList) {
        FunctionEx<Message<String>, TransactionEcommerce> pulsarProjectionFunction =
                message -> {
                    //TODO: A new deserializer for each message, could optimize with shared if thread-safe
                    try (TransactionEcommerceJsonDeserializer transactionJsonDeserializer =
                            new TransactionEcommerceJsonDeserializer()) {
                        byte[] bytes = message.getValue().getBytes(StandardCharsets.UTF_8);
                        return transactionJsonDeserializer.deserialize("", bytes);
                    }
                };

        return (StreamSource<TransactionEcommerce>) pulsarSource(pulsarList, pulsarProjectionFunction);
    }

    /**
     * <p>This is similar to {@link IngestTransactions#IngestTransactions()} but
     * returns a different type.
     * </p>
     *
     * @param pulsarList
     * @return
     */
    @SuppressWarnings("unchecked")
    private static StreamSource<TransactionTrade> pulsarSourceTrade(String pulsarList) {
        FunctionEx<Message<String>, TransactionTrade> pulsarProjectionFunction =
                message -> {
                    //TODO: A new deserializer for each message, could optimize with shared if thread-safe
                    try (TransactionTradeJsonDeserializer transactionJsonDeserializer = new TransactionTradeJsonDeserializer()) {
                        byte[] bytes = message.getValue().getBytes(StandardCharsets.UTF_8);
                        return transactionJsonDeserializer.deserialize("", bytes);
                    }
                };

        return (StreamSource<TransactionTrade>) pulsarSource(pulsarList, pulsarProjectionFunction);
    }

    /**
     * <p>Builds a source for Pulsar
     * </p>
     *
     * @param pulsarList
     * @param pulsarProjectionFunction - Extracts the data
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StreamSource pulsarSource(String pulsarList, FunctionEx pulsarProjectionFunction) {
        String serviceUrl = UtilsUrls.getPulsarServiceUrl(pulsarList);

        SupplierEx<PulsarClient> pulsarConnectionSupplier =
                () -> PulsarClient.builder()
                .connectionTimeout(1, TimeUnit.SECONDS)
                .serviceUrl(serviceUrl)
                .build();

        SupplierEx<Schema<String>> pulsarSchemaSupplier =
                () -> Schema.STRING;

        return PulsarSources.pulsarReaderBuilder(
                        MyConstants.PULSAR_TOPIC_NAME_TRANSACTIONS,
                        pulsarConnectionSupplier,
                        pulsarSchemaSupplier,
                        pulsarProjectionFunction).build();
    }
}
