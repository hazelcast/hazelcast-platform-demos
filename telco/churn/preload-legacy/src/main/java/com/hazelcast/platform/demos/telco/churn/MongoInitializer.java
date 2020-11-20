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

}
