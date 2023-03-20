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

package com.hazelcast.platform.demos.retail.clickstream;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>Entry point. Spring will start Hazelcast and leave it running.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(MyProperties.class)
public class Application {

    /**
     * <p>Check Cassandra address for Docker or Kubernetes before
     * Spring initializes data source.
     * </p>
     */
    static {
        // Spring Boot should set this automatically, but may not if we create the beans
        System.setProperty("hazelcast.logging.type", "slf4j");

        String cassandraContactPoints = System.getProperty("my.cassandra.contact.points", "");
        if (cassandraContactPoints.length() ==  0) {
            // Kubernetes
            System.setProperty("spring.cassandra.contact-points", "clickstream-cassandra.default.svc.cluster.local");
        } else {
            // Docker
            System.setProperty("spring.cassandra.contact-points", cassandraContactPoints);
        }
        System.out.printf("'spring.cassandra.contact-points'=='%s'%n",
                System.getProperty("spring.cassandra.contact-points"));
        // Other props
        List<String> others = List.of("CLUSTER_NAME", "NODE_NAME");
        for (String other : others) {
            System.out.printf("'%s'=='%s'%n", other, System.getProperty(other));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.exit(0);
    }

}
