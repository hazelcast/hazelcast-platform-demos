/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordMetadata;
import com.hazelcast.platform.demos.telco.churn.domain.Customer;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerRepository;

/**
 * <p>Update data in Mongo, can be run multiple times</p>
 */
@Configuration
@EnableMongoRepositories(basePackageClasses = CustomerRepository.class)
public class MongoUpdater implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoUpdater.class);

    private static final String ALPHABET_LOWER_CASE =
            "abcdefghijklmnopqrstuvwxyz".toLowerCase(Locale.ROOT);
    // Connector outputs in batches, ensure enough changes to exceed "mongo.json" queue
    private static final int LOOPS_MAX = 5;

    @Autowired
    private CustomerRepository customerRepository;
    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * <p>Alternate capitalisation</p>
     */
    @Override
    public void run(String... args) throws Exception {
        LOGGER.debug("BEFORE: count()=={}", this.customerRepository.count());

        List<?> resultsProjection = this.customerRepository.findOnlyId();

        // stream.sorted() not ideal for scaling, but TreeSet not much better
        SortedSet<String> keysTmp = (SortedSet<String>) resultsProjection
                .stream()
                .map(str -> {
                    JSONObject json = new JSONObject(str.toString());
                    // The projection prefixes the projected field name with "_"
                    return json.get("_" + CallDataRecordMetadata.ID).toString();
                })
                .collect(Collectors.toCollection(TreeSet::new));
        List<String> keys = new ArrayList<>(keysTmp);

        int count = 0;
        int loop = 0;
        while (loop++ < LOOPS_MAX) {
            LOGGER.info("Loop {}", loop);
            for (int i = 0 ; i < keys.size() ; i++) {
                if ((i % 2) == 0) {
                    boolean success = this.update(keys.get(i));
                    if (success) {
                        count++;
                    }
                }
            }
        }

        if (count == 0) {
            LOGGER.error("updates made=={}", count);
        } else {
            LOGGER.info("updates made=={}", count);
        }

        LOGGER.debug("AFTER:  count()=={}", this.customerRepository.count());
    }

    /**
     * <p>Try an update
     * </p>
     *
     * @param key Should be found as nothing running deletes in this demo
     * @return If successful
     */
    public boolean update(String key) {
        try {
            Customer customer =
                    this.customerRepository.findById(key).get();

            if (customer == null) {
                LOGGER.error("No record to update for key '{}'", key);
                return false;
            } else {
                LOGGER.trace("Change:  {}", customer);

                // Flip upper/lower case
                if (ALPHABET_LOWER_CASE.indexOf(customer.getLastName().charAt(0)) > -1) {
                    customer.setLastName(customer.getLastName().toUpperCase(Locale.ROOT));
                    customer.setFirstName(customer.getFirstName().toUpperCase(Locale.ROOT));
                } else {
                    customer.setLastName(customer.getLastName().toLowerCase(Locale.ROOT));
                    customer.setFirstName(customer.getFirstName().toLowerCase(Locale.ROOT));
                }
                customer.setLastModifiedBy(this.springApplicationName);
                customer.setLastModifiedDate(System.currentTimeMillis());
                String[] notes = customer.getNotes();
                for (int i = 0; i < notes.length; i++) {
                    notes[i] = MyUtils.rot13(notes[i]);
                }
                customer.setNotes(notes);

                this.customerRepository.save(customer);
                LOGGER.trace("Changed: {}", customer);
                return true;
            }

        } catch (Exception e) {
            LOGGER.error(key, e);
            return false;
        }
    }
}
