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

package com.hazelcast.platform.demos.telco.churn;

import java.util.Properties;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
@SpringBootApplication
@EnableConfigurationProperties(MyProperties.class)
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    static {
        Properties properties = new Properties();
        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            properties.put("spring.data.cassandra.contact-points",
                    "churn-cassandra.default.svc.cluster.local");
            properties.put("spring.data.mongodb.host",
                    "churn-mongo.default.svc.cluster.local");
            properties.put("spring.datasource.url",
                    "jdbc:mysql://churn-mysql.default.svc.cluster.local:3306/churn"
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        } else {
            properties.put("spring.data.cassandra.contact-points", "cassandra");
            properties.put("spring.data.mongodb.host", "mongo");
            properties.put("spring.datasource.url",
                    "jdbc:mysql://mysql:3306/churn"
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }
        for (Entry<Object, Object> entry : properties.entrySet()) {
            LOGGER.info("'{}'=='{}'", entry.getKey(), entry.getValue());
            System.setProperty(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    /**
     * <p>Start Jet with specific configuration, and leave it running.
     * </p>
     */
    public static void main(String[] args) throws Exception {

        // Use for importing security config per environment
        String environment = System.getProperty("my.environment");
        if (environment == null || environment.length() == 0) {
            LOGGER.error("Usage: 'my.environment' system property not set");
            System.exit(1);
        }

        // For cluster initialization, defer til expected size
        String initSizeStr = System.getProperty("my.initSize");
        if (initSizeStr == null || initSizeStr.length() == 0) {
            LOGGER.error("Usage: 'my.initSize' system property not set");
            System.exit(1);
        } else {
            try {
                int initSize = Integer.parseInt(initSizeStr);
                if (initSize <= 0) {
                    LOGGER.error("Usage: 'my.initSize' system property negative, '{}'", initSizeStr);
                    System.exit(1);
                }
            } catch (Exception e) {
                LOGGER.error("Usage: 'my.initSize' system property not a number, '{}'", initSizeStr);
                System.exit(1);
            }
        }

        SpringApplication.run(Application.class, args);
    }

}
