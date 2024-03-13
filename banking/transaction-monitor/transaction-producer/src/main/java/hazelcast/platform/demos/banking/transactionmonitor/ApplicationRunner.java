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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
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
    private static final int LOWEST_PAYMENT_QUANTITY = 10_000;
    private static final int HIGHEST_PAYMENT_QUANTITY = 1_000_000;
    private static final int LOWEST_TRADE_QUANTITY = 10;
    private static final int HIGHEST_TRADE_QUANTITY = 10_000;
    private static final int DISCOUNT_ECOMMERCE_PERCENT = 100;
    private static final int DISCOUNT_ECOMMERCE_1 = 75;
    private static final int DISCOUNT_ECOMMERCE_2 = 85;
    private static final int DISCOUNT_ECOMMERCE_3 = 90;
    private static final int ONE_HUNDRED = 100;
    private static final List<String> PAYMENT_CHARGE_BEARER_TYPES
        = List.of("DEBT", "CRED", "SHAR");
    private static final String NEWLINE = System.lineSeparator();

    private final int rate;
    private final int max;
    private final boolean usePulsar;
    private final TransactionMonitorFlavor transactionMonitorFlavor;
    private int count;
    private final KafkaProducer<String, String> kafkaProducer;
    private final Producer<String> pulsarProducer;
    private List<String> bics;
    private int bicsSize;
    private List<String> ecommerceItems;
    private List<String> tradeSymbols;
    private Map<String, Tuple2<String, Double>> bicsToCurrency;
    private Map<String, Double> ecommerceItemsToPrice;
    private Map<String, Integer> tradeSymbolToPrice;
    private String todayCCYYMMDD;

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
            boolean arg4, TransactionMonitorFlavor arg5) throws Exception {
        this.rate = arg0;
        this.max = arg1;
        String bootstrapServers = arg2;
        String pulsarList = arg3;
        this.usePulsar = arg4;
        this.transactionMonitorFlavor = arg5;

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

        this.initData();
    }

    /**
     * <p>Set up data.
     * </p>
     */
    private void initData() throws Exception {
        Map<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>
            nasdaqListed = MyUtils.nasdaqListed();
        Map<String, Tuple4<String, Double, String, String>>
            bicList = MyUtils.bicList();
        Map<String, Tuple3<String, String, Double>>
            productCatalog = MyUtils.productCatalog();

        this.bics = new ArrayList<>(bicList.keySet());
        this.bicsSize = this.bics.size();
        this.ecommerceItems = new ArrayList<>(productCatalog.keySet());
        this.tradeSymbols = new ArrayList<>(nasdaqListed.keySet());
        this.todayCCYYMMDD = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        this.bicsToCurrency = bicList.entrySet().stream()
                .collect(Collectors.<Entry<String,
                         Tuple4<String, Double, String, String>>,
                             String, Tuple2<String, Double>>toMap(
                         entry -> entry.getKey(),
                         entry -> Tuple2.tuple2(entry.getValue().f0(), entry.getValue().f1())
                        ));
        this.ecommerceItemsToPrice = productCatalog.entrySet().stream()
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
                    switch (this.transactionMonitorFlavor) {
                    case ECOMMERCE:
                        transaction = this.createEcommerceTransaction(id, random);
                        break;
                    case PAYMENTS:
                        transaction = this.createPaymentsTransaction(id, random);
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
     * The price may be discounted. The quantity is usually 1 but sometimes higher.
     * <p>
     *
     * @param id Identifies the transaction uniquely
     * @return A transaction with that transaction Id
     */
    private String createEcommerceTransaction(String id, ThreadLocalRandom random) {
        String itemCode = this.ecommerceItems.get(random.nextInt(this.ecommerceItems.size()));

        double price = this.ecommerceItemsToPrice.get(itemCode);
        // Buy 1, sometimes 2, even 3.
        int quantity = 1;
        if (random.nextInt(LOWEST_ECOMMERCE_QUANTITY) == 0) {
            quantity++;
            if (random.nextInt(LOWEST_ECOMMERCE_QUANTITY) == 0) {
                quantity++;
            }
        }

        int discountBand = random.nextInt(DISCOUNT_ECOMMERCE_PERCENT);
        if (discountBand > DISCOUNT_ECOMMERCE_1 && price <= DISCOUNT_ECOMMERCE_2) {
            price = price * DISCOUNT_ECOMMERCE_1 / DISCOUNT_ECOMMERCE_PERCENT;
        }
        if (discountBand > DISCOUNT_ECOMMERCE_2 && price <= DISCOUNT_ECOMMERCE_3) {
            price = price * DISCOUNT_ECOMMERCE_2 / DISCOUNT_ECOMMERCE_PERCENT;
        }
        if (discountBand > DISCOUNT_ECOMMERCE_3) {
            price = price * DISCOUNT_ECOMMERCE_3 / DISCOUNT_ECOMMERCE_PERCENT;
        }

        String transaction = String.format("{"
                        + "\"id\": \"%s\","
                        + "\"timestamp\": %d,"
                        + "\"itemCode\": \"%s\","
                        + "\"price\": %.2f,"
                        + "\"quantity\": %d"
                        + "}",
                id,
                System.currentTimeMillis(),
                itemCode,
                price,
                quantity
        );

        return transaction;
    }

    /**
     * <p>Create a PAIN.001 (Customer Credit Transfer Initiation) message
     * in XML held in a String.
     * </p>
     *
     * @param id
     * @param random
     * @return
     */
    @SuppressFBWarnings(value = "RV_ABSOLUTE_VALUE_OF_HASHCODE",
            justification = "Using HashCode to generate random number")
    private String createPaymentsTransaction(String id, ThreadLocalRandom random) {
        StringBuilder stringBuilder = new StringBuilder();

        String bicCreditor = this.bics.get(random.nextInt(this.bicsSize));
        String ccy = this.bicsToCurrency.get(bicCreditor).f0();
        Double rate = this.bicsToCurrency.get(bicCreditor).f1();
        int usd = random.nextInt(LOWEST_PAYMENT_QUANTITY, HIGHEST_PAYMENT_QUANTITY);
        Double amt = (1 / rate) * usd;
        String chargeBearerType = createChargeBearerType(random);

        // Source and target are different banks, more interesting for monitoring
        // interbank payments than between accounts at same bank, possible AML.
        String bicDebitor = this.bics.get(random.nextInt(this.bicsSize));
        while (bicDebitor.equals(bicCreditor)) {
            bicDebitor = this.bics.get(random.nextInt(this.bicsSize));
        }
        String instrId = bicDebitor + "/" + this.todayCCYYMMDD + "/" + Math.abs(id.hashCode());

        stringBuilder.append("<pain.001.001>").append(NEWLINE);
        stringBuilder.append(" <CdtTrfTxInf>").append(NEWLINE);

        stringBuilder.append("  <Amt>").append(NEWLINE);
        stringBuilder.append("   <InstdAmt Ccy=\"").append(ccy).append("\">")
            .append(String.format("%.2f", amt)).append("</InstdAmt>").append(NEWLINE);
        stringBuilder.append("  </Amt>").append(NEWLINE);

        stringBuilder.append("  <Cdtr>").append(NEWLINE);
        stringBuilder.append("   <Nm>").append(bicDebitor).append("</Nm>").append(NEWLINE);
        stringBuilder.append("  </Cdtr>").append(NEWLINE);

        stringBuilder
            .append("  <CdtrAgt>").append(NEWLINE)
            .append("   <FinInstnId>").append(NEWLINE)
            .append("    <BICFI>").append(bicCreditor).append("</BICFI>").append(NEWLINE)
            .append("   </FinInstnId>").append(NEWLINE)
            .append("  </CdtrAgt>").append(NEWLINE);

        stringBuilder.append("  <ChrgBr>").append(chargeBearerType)
            .append("</ChrgBr>").append(NEWLINE);

        stringBuilder
            .append("  <PmntId>").append(NEWLINE)
            .append("   <InstrId>").append(instrId).append("</InstrId>").append(NEWLINE)
            .append("   <EndToEndId>").append(id).append("</EndToEndId>").append(NEWLINE)
            .append("  </PmntId>").append(NEWLINE);

        stringBuilder.append(" </CdtTrfTxInf>").append(NEWLINE);
        stringBuilder.append("</pain.001.001>");

        String transaction = String.format("{"
                + "\"id\": \"%s\","
                + "\"timestamp\": %d,"
                + "\"kind\": \"pain.001.001\","
                + "\"bicCreditor\": \"%s\","
                + "\"bicDebitor\": \"%s\","
                + "\"ccy\": \"%s\","
                // No decimal places, only interested in main currency
                + "\"amtFloor\": %.0f.00,"
                + "\"xml\": %s"
                + "}",
                id,
                System.currentTimeMillis(),
                bicCreditor,
                bicDebitor,
                ccy,
                amt,
                MyUtils.xmlSafeForJson(stringBuilder.toString())
        );

        return transaction;
    }

    /**
     * <p>Pick a type, Credit, Debit or Shared
     * </p>
     *
     * @param random
     * @return
     */
    private String createChargeBearerType(ThreadLocalRandom random) {
        int i = random.nextInt(PAYMENT_CHARGE_BEARER_TYPES.size());
        return PAYMENT_CHARGE_BEARER_TYPES.get(i);
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
        double price = 1D * this.tradeSymbolToPrice.compute(symbol,
                (k, v) -> v + random.nextInt(-1, 2)) / ONE_HUNDRED;

        String transaction = String.format("{"
                        + "\"id\": \"%s\","
                        + "\"timestamp\": %d,"
                        + "\"symbol\": \"%s\","
                        + "\"price\": %.2f,"
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
