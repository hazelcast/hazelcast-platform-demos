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

package com.hazelcast.platform.demos.telco.churn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 * <p>We use module "{@code common-cassandra}" for domain object, but
 * don't connect to Cassandra so exclude repository bean which would
 * fail if instantiated.
 * </p>
 */
@SpringBootApplication(exclude = {
        CassandraAutoConfiguration.class
        })
@EnableConfigurationProperties(MyProperties.class)
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    /**
     * <p>Start, and keep running until killed.
     * </p>
     */
    public static void main(String[] args) throws Exception {

        // To connect to Kafka brokers
        String bootstrapServers = System.getProperty("my.bootstrap.servers");
        if (bootstrapServers == null || bootstrapServers.length() == 0) {
            LOGGER.error("Usage: 'my.bootstrap.servers' system property not set");
            System.exit(1);
        }

        SpringApplication.run(Application.class, args);
    }

}
