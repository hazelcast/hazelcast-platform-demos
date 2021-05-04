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

package com.hazelcast.platform.demos.telco.churn;

import java.util.Map.Entry;
import java.util.Properties;

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
     * <p>Run all the initializers in an unspecified order
     * then shutdown.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
        System.exit(0);
    }

}
