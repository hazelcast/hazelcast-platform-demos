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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.Customer;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerRepository;
import com.hazelcast.platform.demos.telco.churn.testdata.CustomerTestdata;
import com.hazelcast.platform.demos.telco.churn.testdata.TariffTestdata;

/**
 * <p>Inserts 100 customer records into Mongo
 * </p>
 */
@Configuration
@EnableMongoRepositories(basePackageClasses = CustomerRepository.class)
public class MongoInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoInitializer.class);
    private static final int PAD_LINES = 3;

    @Autowired
    private CustomerRepository customerRepository;
    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * <p>Generate one customer from each combination of
     * first name and last name.
     * </p>
     */
    @Override
    public void run(String... args) throws Exception {
        long now = System.currentTimeMillis();
        long before = this.customerRepository.count();
        LOGGER.info("BEFORE: count()=={}", before);

        // Alternate tariffs from current year
        Object[][] tariffs = TariffTestdata.getTariffs();
        String[] tariffName = new String[2];
        tariffName[0] = tariffs[0][1].toString();
        tariffName[1] = tariffs[1][1].toString();

        int insert = 0;
        int maxNotes = CustomerTestdata.getFirstNames().length;
        for (String firstName : CustomerTestdata.getFirstNames()) {
            for (String lastName : CustomerTestdata.getLastNames()) {
                Customer customer = new Customer();
                customer.setAccountType(tariffName[insert % 2]);
                customer.setFirstName(firstName);
                customer.setLastName(lastName);
                customer.setId(MyUtils.getTelno(insert));
                customer.setCreatedBy(this.springApplicationName);
                customer.setCreatedDate(now);
                customer.setLastModifiedBy(this.springApplicationName);
                customer.setLastModifiedDate(now);

                customer.setNotes(this.createNotes(insert, 1 + (insert % maxNotes)));
                this.customerRepository.save(customer);
                insert++;
            }
        }

        long after = this.customerRepository.count();
        if ((before + insert) == after) {
            LOGGER.info("AFTER:  count()=={}", after);
        } else {
            LOGGER.warn("AFTER:  count()=={}, but {} inserts", after, insert);
        }
    }

    /**
     * <p>Create some notes on the customer, to simulate what Call Center
     * staff members may have logged each time the customer calls.
     * </p>
     * <p>As a lengthy String, this pushed the frequency of Mongo CDC
     * oplog rollovers up. Minimum oplog size is 1GB in Mongo 4.2.
     * </p>
     *
     * @param insert Insert number
     * @param max How many lines
     * @return A big String
     */
    public String createNotes(int insert, int max) {
        TreeSet<String> notes = new TreeSet<>(Collections.reverseOrder());

        for (int i = max ; i > 0 ; i--) {
            LocalDateTime when = LocalDateTime.now().minusDays(i);
            notes.add(String.format("(%d, %s) : ", i, when));
            for (int j = 1 ; j < PAD_LINES ; j++) {
                notes.add(String.format("(%d, %s) : padding %d yada yada yada", i, when, (0 - j)));
            }
        }

        StringBuffer result = new StringBuffer();
        notes.stream().forEach(note -> result.append(note).append(MyUtils.NEWLINE));
        return result.toString();
    }
}
