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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.function.SupplierEx;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.contrib.pulsar.PulsarSources;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.platform.demos.utils.UtilsUrls;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Creates a Jet pipeline to upload from a Kafka topic into a
 * Hazelcast map.
 */
public class IngestTransactions {

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
    public static Pipeline buildPipeline(String bootstrapServers, String pulsarList, boolean usePulsar,
            TransactionMonitorFlavor transactionMonitorFlavor) {

        Properties properties = InitializerConfig.kafkaSourceProperties(bootstrapServers);

        Pipeline pipeline = Pipeline.create();

        StreamStage<Entry<String, HazelcastJsonValue>> inputSource;
        if (usePulsar) {
            inputSource =
                    pipeline.readFrom(IngestTransactions.pulsarSource(pulsarList))
                    .withoutTimestamps();
        } else {
            inputSource =
                    pipeline.readFrom(KafkaSources.<String, String, Entry<String, HazelcastJsonValue>>
                        kafka(properties,
                        record -> Util.entry(record.key(), new HazelcastJsonValue(record.value())),
                        MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS)
                        )
                 .withoutTimestamps();
        }

        if (transactionMonitorFlavor == TransactionMonitorFlavor.PAYMENTS) {
            inputSource
            .map(IngestTransactions::depleteEntry).setName("deplete-entry")
            .writeTo(Sinks.map(MyConstants.IMAP_NAME_TRANSACTIONS));
        } else {
            inputSource
            .writeTo(Sinks.map(MyConstants.IMAP_NAME_TRANSACTIONS));
        }

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

        /* Bonus output fork depending on flavor
         */
        if (transactionMonitorFlavor == TransactionMonitorFlavor.PAYMENTS) {
            inputSource
            .map(IngestTransactions::makeEntryXML).setName("extract-xml")
            .writeTo(Sinks.map(MyConstants.IMAP_NAME_TRANSACTIONS_XML));
        }

        return pipeline;
    }

    /**
     * <p>This is similar to {@link AggregateQuery#IngestTransactions()} but
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
                    String transaction = message.getValue();
                    HazelcastJsonValue value = new HazelcastJsonValue(transaction);
                    return new SimpleImmutableEntry<>(key, value);
                };

        return PulsarSources.pulsarReaderBuilder(
            MyConstants.PULSAR_TOPIC_NAME_TRANSACTIONS,
            pulsarConnectionSupplier,
            pulsarSchemaSupplier,
            pulsarProjectionFunction).build();
    }

    /**
     * <p>Simplify the JSON, removing nested array, so it can be more easily queried.
     * See {@link #makeEntryXML(Entry)} that does capture the XML part for storage
     * elsewhere.
     * </p>
     * @param input
     * @return
     */
    @SuppressFBWarnings(value = "", justification = "JSON access can throw exception")
    private static Entry<String, HazelcastJsonValue> depleteEntry(Entry<String, HazelcastJsonValue> input) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        try {
            JSONObject json = new JSONObject(input.getValue().toString());
            stringBuilder.append(" \"id\" : \"").append(json.getString("id")).append("\"");
            stringBuilder.append(", \"timestamp\" : ").append(json.getLong("timestamp"));
            stringBuilder.append(", \"kind\" : \"").append(json.getString("kind")).append("\"");
            stringBuilder.append(", \"bicCreditor\" : \"").append(json.getString("bicCreditor")).append("\"");
            stringBuilder.append(", \"bicDebitor\" : \"").append(json.getString("bicDebitor")).append("\"");
            stringBuilder.append(", \"ccy\" : \"").append(json.getString("ccy")).append("\"");
            stringBuilder.append(", \"amtFloor\" : ").append(json.getDouble("amtFloor"));
        } catch (Exception e) {
            // Don't log, if running in Viridian user may not download logs. Null means no entry passed to next stage, filter.
            return null;
        }
        stringBuilder.append("}");
        return Tuple2.tuple2(input.getKey(), new HazelcastJsonValue(stringBuilder.toString()));
    }

    /**
     * <p>The payment is passed as JSON, but XML is multi-line. Turn XML back
     * from an array of strings into a single multi-line string.
     * </p>
     *
     * @param input
     * @return
     */
    @SuppressFBWarnings(value = "", justification = "JSON access can throw exception")
    private static Entry<String, String> makeEntryXML(Entry<String, HazelcastJsonValue> input) {
        String xml;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            JSONObject json = new JSONObject(input.getValue().toString());
            JSONArray array = json.getJSONArray("xml");
            for (int i = 0; i < array.length(); i++) {
                if (i > 0) {
                    stringBuilder.append(System.lineSeparator());
                }
                stringBuilder.append(array.getString(i));
            }
            xml = stringBuilder.toString();
        } catch (Exception e) {
            // Don't log, if running in Viridian user may not download logs
            xml = input.getValue().toString();
        }
        return Tuple2.tuple2(input.getKey(), xml);
    }
}
