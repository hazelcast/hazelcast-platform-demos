/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    /**
     * <p>Configure Hazelcast logging via Slf4j. Implementation
     * in "{@code pom.xml}" is Logback.
     * </p>
     * <p>Set this before Hazelcast starts rather than in
     * "{@code hazelcast.yml}", otherwise some log messages
     * are produced before "{@code hazelcast.yml}" is read
     * dictating the right logging framework to use.
     * </p>
     */
    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
    }

    /**
     * <p>Start Jet with specific configuration, and leave it running.
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

        Config config = ApplicationConfig.buildConfig();

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        String initializerProperty = "my.initialize";
        if (System.getProperty(initializerProperty, "").equalsIgnoreCase(Boolean.TRUE.toString())) {
            ApplicationInitializer.initialise(hazelcastInstance, bootstrapServers);
        } else {
            LOGGER.info("Skip initialize as '{}'=='{}', assume client will do so",
                    initializerProperty, System.getProperty(initializerProperty));
        }
    }

}
