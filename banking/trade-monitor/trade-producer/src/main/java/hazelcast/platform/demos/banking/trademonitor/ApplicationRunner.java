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

/**
 * <p>The main "{@code run()}" method of the application, called
 * once configuration created.
 * </p>
 */
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);

    private static final long LOG_THRESHOLD = 20_000L;
    private static final int MAX_BATCH_SIZE = 16 * 1024;
    private static final int OPENING_PRICE = 2_500;
    private static final int LOWEST_QUANTITY = 10;
    private static final int HIGHEST_QUANTITY = 10_000;

    private final int rate;
    private final int max;
    private final boolean usePulsar;
    private int count;
    private final KafkaProducer<String, String> kafkaProducer;
    private final Producer<String> pulsarProducer;
    private final List<String> symbols;
    private final Map<String, Integer> symbolToPrice;

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
     */
    public ApplicationRunner(int arg0, int arg1, String arg2, String arg3, boolean arg4) throws Exception {
        this.rate = arg0;
        this.max = arg1;
        String bootstrapServers = arg2;
        String pulsarList = arg3;
        this.usePulsar = arg4;

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
                    .topic(MyConstants.PULSAR_TOPIC_NAME_TRADES)
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

        Map<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>
            nasdaqListed = MyUtils.nasdaqListed();

        this.symbols = new ArrayList<>(nasdaqListed.keySet());

        this.symbolToPrice = nasdaqListed.entrySet().stream()
                .collect(Collectors.<Entry<String,
                        Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>,
                            String, Integer>toMap(
                        entry -> entry.getKey(),
                        entry -> OPENING_PRICE));
    }

    /**
     * <p>Loop, producing random trades at the requested
     * rate.
     * </p>
     *
     * @throws Exception
     */
    public void run() throws Exception {
        if (this.max > 0) {
            LOGGER.info("Producing {} trades per second, until {} written", this.rate, this.max);
        } else {
            LOGGER.info("Producing {} trades per second", this.rate);
        }

        long interval = TimeUnit.SECONDS.toNanos(1) / this.rate;
        long emitSchedule = System.nanoTime();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // loop over ( wait, create a random trade Id, emit )
        try {
            while (this.max <= 0 || this.count < this.max) {
                for (int i = 0; i < MAX_BATCH_SIZE; i++) {
                    if (System.nanoTime() < emitSchedule) {
                        break;
                    }

                    String id = UUID.randomUUID().toString();
                    String trade = this.createTrade(id, random);

                    if (this.usePulsar) {
                        this.pulsarProducer.newMessage(Schema.STRING)
                            .key(id)
                            .value(trade)
                            .send();
                    } else {
                        this.kafkaProducer.send(new ProducerRecord<>(MyConstants.KAFKA_TOPIC_NAME_TRADES, id, trade));
                    }

                    if (this.count % LOG_THRESHOLD == 0) {
                        LOGGER.info("Wrote {} => \"{}\"", this.count, trade);
                    }
                    this.count++;

                    emitSchedule += interval;
                }

                TimeUnit.MILLISECONDS.sleep(1L);
            }
        } catch (InterruptedException exception) {
            this.kafkaProducer.close();
        }

        LOGGER.info("Produced {} trades", this.count);
    }

    /**
     * <p>Create a random trade with the supplied (random!) Id.
     * The price is a random fluctation of the price of the
     * symbol. The quantity is random but within bounds.
     * <p>
     *
     * @param id Identifies the trade uniquely
     * @return A trade with that trade Id
     */
    private String createTrade(String id, ThreadLocalRandom random) {
        String symbol = symbols.get(random.nextInt(symbols.size()));

        // Vary price between -1 to +2... randomly
        int price = this.symbolToPrice.compute(symbol,
                (k, v) -> v + random.nextInt(-1, 2));

        String trade = String.format("{"
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
                random.nextInt(LOWEST_QUANTITY, HIGHEST_QUANTITY)
        );

        return trade;
    }

}
