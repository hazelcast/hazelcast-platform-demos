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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.Customer;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerRepository;

/**
 * <p>Inserts customer records into Mongo
 * </p>
 */
@Configuration
@EnableMongoRepositories(basePackageClasses = CustomerRepository.class)
public class MongoInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoInitializer.class);

    @Autowired
    private CustomerRepository customerRepository;
    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * XXX
     */
    @Override
    public void run(String... args) throws Exception {
        LOGGER.debug("BEFORE: count()=={}", this.customerRepository.count());

        long now = System.currentTimeMillis();

        Customer c0 = new Customer();
        c0.setAccountType("?");
        c0.setFirstName("n");
        c0.setLastName("s");
        c0.setId("b");
        c0.setCreatedBy(this.springApplicationName);
        c0.setCreatedDate(now);
        c0.setLastModifiedBy(this.springApplicationName);
        c0.setLastModifiedDate(now);
        this.customerRepository.save(c0);

        Customer c1 = new Customer();
        c1.setAccountType("?");
        c1.setFirstName("neil");
        c1.setLastName("stevenson");
        c1.setId("a");
        c1.setCreatedBy(this.springApplicationName);
        c1.setCreatedDate(now);
        c1.setLastModifiedBy(this.springApplicationName);
        c1.setLastModifiedDate(now);
        this.customerRepository.save(c1);


        LOGGER.debug("AFTER:  count()=={}", this.customerRepository.count());
    }

}
