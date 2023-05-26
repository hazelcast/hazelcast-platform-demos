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

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.platform.demos.utils.UtilsProperties;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final int DEFAULT_RATE = 300;
    private static final int DEFAULT_MAX = -1;

    /**
     * <p>To run we need one argument, the Kafka brokers to connect
     * to. The second argument, the rate at which to create transactions is
     * optional. A third argument, the number of items to produce,
     * is also optional, and defaults to unlimited.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        Properties properties = UtilsProperties.loadClasspathProperties("application.properties");
        String pulsarOrKafka = properties.getProperty(MyConstants.PULSAR_OR_KAFKA_KEY);
        TransactionMonitorFlavor transactionMonitorFlavor = MyUtils.getTransactionMonitorFlavor(properties);

        boolean usePulsar = MyUtils.usePulsar(pulsarOrKafka);
        if (usePulsar) {
            LOGGER.info("Using Pulsar = '{}'=='{}'",
                    MyConstants.PULSAR_OR_KAFKA_KEY, pulsarOrKafka);
        } else {
            LOGGER.info("Using Kafka = '{}'=='{}'",
                    MyConstants.PULSAR_OR_KAFKA_KEY, pulsarOrKafka);
        }
        LOGGER.info("TransactionMonitorFlavor=='{}'", transactionMonitorFlavor);

        String bootstrapServers = null;
        String pulsarAddress = null;

        if (args.length == 1) {
            bootstrapServers = args[0];
            pulsarAddress = args[0];
        } else {
            bootstrapServers = System.getProperty("my.bootstrap.servers", "");
            pulsarAddress = System.getProperty("my.pulsar.address", "");
            if ((bootstrapServers.isBlank() && !usePulsar)
                    || (pulsarAddress.isBlank() && usePulsar)) {
                LOGGER.error("Usage: 1 arg expected: bootstrapServers or pulsarAddress");
                LOGGER.error("eg: 127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094 / 127.0.0.1:6650");
                System.exit(1);
            }
        }
        LOGGER.info("bootstrapServers={}", bootstrapServers);
        LOGGER.info("pulsarAddress={}", pulsarAddress);

        int rate = DEFAULT_RATE;
        int max = DEFAULT_MAX;
        if (args.length >= 2) {
            rate = Integer.parseInt(args[1]);
        }
        if (args.length == 3) {
            max = Integer.parseInt(args[2]);
        }

        new ApplicationRunner(rate, max, bootstrapServers, pulsarAddress, usePulsar, transactionMonitorFlavor).run();
    }

}
