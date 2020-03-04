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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final int DEFAULT_RATE = 300;

    /**
     * <p>To run we need one argument, the Kafka brokers to connect
     * to. The second argument, the rate at which to create trades is
     * optional.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        String bootstrapServers = null;

        if (args.length == 1) {
            bootstrapServers = args[0];
        } else {
            bootstrapServers = System.getProperty("my.bootstrap.servers");
            if (bootstrapServers == null || bootstrapServers.length() == 0) {
                LOGGER.error("Usage: 1 arg expected: bootstrapServers");
                LOGGER.error("eg: 127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094");
                System.exit(1);
            }
        }

        int rate = DEFAULT_RATE;
        if (args.length == 2) {
            rate = Integer.parseInt(args[1]);
        }

        new ApplicationRunner(rate, bootstrapServers).run();
    }

}
