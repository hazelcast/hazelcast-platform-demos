/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.function.SupplierEx;
import com.hazelcast.jet.contrib.pulsar.PulsarSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.utils.UtilsUrls;

/**
 * <p>Provides input sources (the start of a pipeline) using Pulsar as the
 * source.
 * </p>
 */
public class MyPulsarSource {

    /**
     * <p>Return a string key and a string value that is converted to JSON.
     * </p>
     *
     * @param pulsarAddress Connection endpoint(s)
     * @return
     */
    @SuppressWarnings("unchecked")
    public static StreamStage<Entry<String, HazelcastJsonValue>> inputSourceKeyAndJson(String pulsarAddress) {
        FunctionEx<Message<String>, Entry<String, HazelcastJsonValue>> pulsarProjectionFunction =
                message -> {
                    String key = message.getKey();
                    String transaction = message.getValue();
                    HazelcastJsonValue value = new HazelcastJsonValue(transaction);
                    return new SimpleImmutableEntry<>(key, value);
                };

        Pipeline pipeline = Pipeline.create();

        return pipeline
                .readFrom(MyPulsarSource.pulsarSource(pulsarAddress, pulsarProjectionFunction))
               .withoutTimestamps();
    }

    /**
     * <p>Return a value formatted as a Java object from a set of known types.
     * </p>
     *
     * @param pulsarAddress Connection endpoint(s)
     * @return
     */
    public static StreamStage<?> inputSourceTransaction(String pulsarAddress,
            TransactionMonitorFlavor transactionMonitorFlavor) {

        Pipeline pipeline = Pipeline.create();

        StreamSource<?> pulsarSource;
        switch (transactionMonitorFlavor) {
        case ECOMMERCE:
            pulsarSource = pulsarSourceEcommerce(pulsarAddress);
            break;
        case PAYMENTS:
            pulsarSource = pulsarSourcePayments(pulsarAddress);
            break;
        case TRADE:
        default:
            pulsarSource = pulsarSourceTrade(pulsarAddress);
            break;
        }

        return pipeline
                .readFrom(pulsarSource)
               .withoutTimestamps();
    }

    /**
     * <p>This is similar to {@link IngestTransactions#IngestTransactions()} but
     * returns a different type.
     * </p>
     *
     * @param pulsarAddress
     * @return
     */
    @SuppressWarnings("unchecked")
    private static StreamSource<TransactionEcommerce> pulsarSourceEcommerce(String pulsarAddress) {
        FunctionEx<Message<String>, TransactionEcommerce> pulsarProjectionFunction =
                message -> {
                    // A new deserializer for each message, could optimize with shared if thread-safe
                    try (TransactionEcommerceJsonDeserializer transactionJsonDeserializer =
                            new TransactionEcommerceJsonDeserializer()) {
                        byte[] bytes = message.getValue().getBytes(StandardCharsets.UTF_8);
                        return transactionJsonDeserializer.deserialize("", bytes);
                    }
                };

        return pulsarSource(pulsarAddress, pulsarProjectionFunction);
    }

    /**
     * <p>This is similar to {@link IngestTransactions#IngestTransactions()} but
     * returns a different type.
     * </p>
     *
     * @param pulsarAddress
     * @return
     */
    @SuppressWarnings("unchecked")
    private static StreamSource<TransactionPayments> pulsarSourcePayments(String pulsarAddress) {
        FunctionEx<Message<String>, TransactionPayments> pulsarProjectionFunction =
                message -> {
                    // A new deserializer for each message, could optimize with shared if thread-safe
                    try (TransactionPaymentsJsonDeserializer transactionJsonDeserializer =
                            new TransactionPaymentsJsonDeserializer()) {
                        byte[] bytes = message.getValue().getBytes(StandardCharsets.UTF_8);
                        return transactionJsonDeserializer.deserialize("", bytes);
                    }
                };

        return pulsarSource(pulsarAddress, pulsarProjectionFunction);
    }

    /**
     * <p>This is similar to {@link IngestTransactions#IngestTransactions()} but
     * returns a different type.
     * </p>
     *
     * @param pulsarAddress
     * @return
     */
    @SuppressWarnings("unchecked")
    private static StreamSource<TransactionTrade> pulsarSourceTrade(String pulsarAddress) {
        FunctionEx<Message<String>, TransactionTrade> pulsarProjectionFunction =
                message -> {
                    // A new deserializer for each message, could optimize with shared if thread-safe
                    try (TransactionTradeJsonDeserializer transactionJsonDeserializer = new TransactionTradeJsonDeserializer()) {
                        byte[] bytes = message.getValue().getBytes(StandardCharsets.UTF_8);
                        return transactionJsonDeserializer.deserialize("", bytes);
                    }
                };

        return pulsarSource(pulsarAddress, pulsarProjectionFunction);
    }

    /**
     * <p>Builds a source for Pulsar
     * </p>
     *
     * @param pulsarAddress
     * @param pulsarProjectionFunction - Extracts the data
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static StreamSource pulsarSource(String pulsarAddress, FunctionEx pulsarProjectionFunction) {
        String serviceUrl = UtilsUrls.getPulsarServiceUrl(pulsarAddress);

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
