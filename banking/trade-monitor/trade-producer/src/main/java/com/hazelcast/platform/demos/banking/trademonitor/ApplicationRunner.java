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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Entry point, "{@code main()}" method.
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
    private final KafkaProducer<String, String> kafkaProducer;
    private final List<String> symbols;
    private final Map<String, Integer> symbolToPrice;

    /**
     * <p>Initialise a connection to Kafka for writing.
     * Obtain the stock symbols to generate. Make the
     * opening price for each $2500.
     * </p>
     *
     * @param arg0 Rate to produce per second
     * @param arg1 Kafka broker list
     */
    public ApplicationRunner(int arg0, String arg1) throws Exception {
        this.rate = arg0;

        String bootstrapServers = arg1;

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());

        this.kafkaProducer = new KafkaProducer<>(properties);

        this.symbols = new ArrayList<>(MyUtils.nasdaqListed().keySet());

        this.symbolToPrice = this.symbols.stream()
                .collect(Collectors.toMap(symbol -> symbol, symbol -> OPENING_PRICE));
    }

    /**
     * <p>Infinite loop, producing random trades at the requested
     * rate.
     * </p>
     *
     * @throws Exception
     */
    public void run() {
        LOGGER.info("Producing {} trades per second", rate);

        long count = 0;
        long interval = TimeUnit.SECONDS.toNanos(1) / rate;
        long emitSchedule = System.nanoTime();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // loop over ( wait, create a random trade Id, emit )
        try {
            while (true) {
                for (int i = 0; i < MAX_BATCH_SIZE; i++) {
                    if (System.nanoTime() < emitSchedule) {
                        break;
                    }

                    String id = UUID.randomUUID().toString();
                    String trade = this.createTrade(id, random);

                    this.kafkaProducer.send(new ProducerRecord<>(MyConstants.KAFKA_TOPIC_NAME_TRADES, id, trade));

                    if (count++ % LOG_THRESHOLD == 0) {
                        LOGGER.info("Wrote {} => \"{}\"", count - 1, trade);
                    }

                    emitSchedule += interval;
                }

                TimeUnit.MILLISECONDS.sleep(1L);
            }
        } catch (InterruptedException exception) {
            this.kafkaProducer.close();
        }
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
