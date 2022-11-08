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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.platform.demos.utils.UtilsUrls;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>The main "{@code run()}" method of the application, called
 * once configuration created.
 * </p>
 */
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);

    private static final long LOG_THRESHOLD = 20_000L;
    private static final int MAX_BATCH_SIZE = 16 * 1024;
    private static final int OPENING_TRADE_PRICE = 2_500;
    private static final int LOWEST_ECOMMERCE_QUANTITY = 10;
    private static final int LOWEST_TRADE_QUANTITY = 10;
    private static final int HIGHEST_TRADE_QUANTITY = 10_000;

    private final int rate;
    private final int max;
    private final boolean usePulsar;
    private final TransactionMonitorSkin transactionMonitorSkin;
    private int count;
    private final KafkaProducer<String, String> kafkaProducer;
    private final Producer<String> pulsarProducer;
    private List<String> ecommerceSymbols;
    private List<String> tradeSymbols;
    private Map<String, Double> ecommerceSymbolToPrice;
    private Map<String, Integer> tradeSymbolToPrice;

    /**
     * <p>Initialise a connection to Kafka for writing.
     * Obtain the stock symbols to generate. Make the
     * opening price for each $2500.
     * </p>
     *
     * @param arg0 Rate to produce per second
     * @param arg1 Maximum to produce before ending
     * @param arg2 Kafka broker list
     * @param arg3 Pulsar connection list
     * @param arg4 Which of arg2 or arg3 to use
     * @param arg5 What type of transactions - banking, e-commerce, etc
     */
    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
            justification = "https://github.com/spotbugs/spotbugs/issues/1812")
    public ApplicationRunner(int arg0, int arg1, String arg2, String arg3,
            boolean arg4, TransactionMonitorSkin arg5) throws Exception {
        this.rate = arg0;
        this.max = arg1;
        String bootstrapServers = arg2;
        String pulsarList = arg3;
        this.usePulsar = arg4;
        this.transactionMonitorSkin = arg5;

        if (this.usePulsar) {
            String serviceUrl = UtilsUrls.getPulsarServiceUrl(pulsarList);
            LOGGER.info("serviceUrl='{}'", serviceUrl);

            PulsarClient pulsarClient =
                    PulsarClient
                    .builder()
                    .connectionTimeout(1, TimeUnit.SECONDS)
                    .serviceUrl(serviceUrl)
                    .build();

            this.pulsarProducer = pulsarClient.newProducer(Schema.STRING)
                    .topic(MyConstants.PULSAR_TOPIC_NAME_TRANSACTIONS)
                    .create();

            this.kafkaProducer = null;
        } else {
            Properties properties = new Properties();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());

            this.kafkaProducer = new KafkaProducer<>(properties);
            this.pulsarProducer = null;
        }

        this.initMaps();
    }

    /**
     * <p>Set up data.
     * </p>
     */
    private void initMaps() throws Exception {
        Map<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>
            nasdaqListed = MyUtils.nasdaqListed();
        Map<String, Tuple3<String, String, Double>>
            productCatalog = MyUtils.productCatalog();

        this.ecommerceSymbols = new ArrayList<>(productCatalog.keySet());
        this.tradeSymbols = new ArrayList<>(nasdaqListed.keySet());

        this.ecommerceSymbolToPrice = productCatalog.entrySet().stream()
                .collect(Collectors.<Entry<String,
                        Tuple3<String, String, Double>>,
                            String, Double>toMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue().f2()));
        this.tradeSymbolToPrice = nasdaqListed.entrySet().stream()
                .collect(Collectors.<Entry<String,
                        Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>,
                            String, Integer>toMap(
                        entry -> entry.getKey(),
                        entry -> OPENING_TRADE_PRICE));
    }

    /**
     * <p>Loop, producing random transactions at the requested
     * rate.
     * </p>
     *
     * @throws Exception
     */
    public void run() throws Exception {
        if (this.max > 0) {
            LOGGER.info("Producing {} transactions per second, until {} written", this.rate, this.max);
        } else {
            LOGGER.info("Producing {} transactions per second", this.rate);
        }

        long interval = TimeUnit.SECONDS.toNanos(1) / this.rate;
        long emitSchedule = System.nanoTime();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // loop over ( wait, create a random transaction, emit )
        try {
            while (this.max <= 0 || this.count < this.max) {
                for (int i = 0; i < MAX_BATCH_SIZE; i++) {
                    if (System.nanoTime() < emitSchedule) {
                        break;
                    }

                    String id = UUID.randomUUID().toString();
                    String transaction;
                    switch (this.transactionMonitorSkin) {
                    case ECOMMERCE:
                        transaction = this.createEcommerceTransaction(id, random);
                        break;
                    case TRADE:
                    default:
                        transaction = this.createTradeTransaction(id, random);
                        break;
                    }

                    if (this.usePulsar) {
                        this.pulsarProducer.newMessage(Schema.STRING)
                            .key(id)
                            .value(transaction)
                            .send();
                    } else {
                        this.kafkaProducer.send(new ProducerRecord<>(MyConstants.KAFKA_TOPIC_NAME_TRANSACTIONS, id, transaction));
                    }

                    if (this.count % LOG_THRESHOLD == 0) {
                        LOGGER.info("Wrote {} => \"{}\"", this.count, transaction);
                    }
                    this.count++;

                    emitSchedule += interval;
                }

                TimeUnit.MILLISECONDS.sleep(1L);
            }
        } catch (InterruptedException exception) {
            this.kafkaProducer.close();
        }

        LOGGER.info("Produced {} transactions", this.count);
    }

    /**
     * <p>Create a random e-commerce transaction with the supplied (random!) Id.
     * The price is fixed. The quantity is usually 1 but sometimes higher.
     * <p>
     *
     * @param id Identifies the transaction uniquely
     * @return A transaction with that transaction Id
     */
    private String createEcommerceTransaction(String id, ThreadLocalRandom random) {
        String symbol = this.ecommerceSymbols.get(random.nextInt(this.ecommerceSymbols.size()));

        double price = this.ecommerceSymbolToPrice.get(symbol);
        // Buy 1, sometimes 2, even 3.
        int quantity = 1;
        if (random.nextInt(LOWEST_ECOMMERCE_QUANTITY) == 0) {
            quantity++;
            if (random.nextInt(LOWEST_ECOMMERCE_QUANTITY) == 0) {
                quantity++;
            }
        }

        String transaction = String.format("{"
                        + "\"id\": \"%s\","
                        + "\"timestamp\": %d,"
                        + "\"symbol\": \"%s\","
                        + "\"price\": %f,"
                        + "\"quantity\": %d"
                        + "}",
                id,
                System.currentTimeMillis(),
                symbol,
                price,
                quantity
        );

        return transaction;
    }

    /**
     * <p>Create a random transaction with the supplied (random!) Id.
     * The price is a random fluctuation of the price of the
     * symbol. The quantity is random but within bounds.
     * <p>
     *
     * @param id Identifies the transaction uniquely
     * @return A transaction with that transaction Id
     */
    private String createTradeTransaction(String id, ThreadLocalRandom random) {
        String symbol = this.tradeSymbols.get(random.nextInt(this.tradeSymbols.size()));

        // Vary price between -1 to +2... randomly
        int price = this.tradeSymbolToPrice.compute(symbol,
                (k, v) -> v + random.nextInt(-1, 2));

        String transaction = String.format("{"
                        + "\"id\": \"%s\","
                        + "\"timestamp\": %d,"
                        + "\"symbol\": \"%s\","
                        + "\"price\": %d,"
                        + "\"quantity\": %d"
                        + "}",
                id,
                System.currentTimeMillis(),
                symbol,
                price,
                random.nextInt(LOWEST_TRADE_QUANTITY, HIGHEST_TRADE_QUANTITY)
        );

        return transaction;
    }

}
