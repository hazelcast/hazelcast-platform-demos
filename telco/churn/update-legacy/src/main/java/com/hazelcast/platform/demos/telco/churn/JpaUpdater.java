/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.Tariff;
import com.hazelcast.platform.demos.telco.churn.domain.TariffRepository;

/**
 * <p>Update data in MySql, can be run multiple times</p>
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = TariffRepository.class)
public class JpaUpdater implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaUpdater.class);

    @Autowired
    private TariffRepository tariffRepository;

    /**
     * <p>Update all tariffs</p>
     */
    @Override
    public void run(String... args) throws Exception {
        LOGGER.debug("BEFORE: count()=={}", this.tariffRepository.count());

        int year = LocalDate.now().getYear();

        List<String> listNow = this.tariffRepository.findThisYearsTariffs(year);

        // stream.sorted() not ideal for scaling, but TreeSet not much better
        SortedSet<String> keysTmp = (SortedSet<String>) listNow
                .stream()
                .collect(Collectors.toCollection(TreeSet::new));
        List<String> keys = new ArrayList<>(keysTmp);

        int count = 0;
        for (int i = 0 ; i < keys.size() ; i++) {
            if ((i % 2) == 0) {
                boolean success = this.update(keys.get(i));
                if (success) {
                    count++;
                }
            }
        }

        if (count == 0) {
            LOGGER.error("updates made=={}", count);
        } else {
            LOGGER.info("updates made=={}", count);
        }

        LOGGER.debug("AFTER:  count()=={}", this.tariffRepository.count());
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
            Tariff tariff =
                    this.tariffRepository.findById(key).get();

            if (tariff == null) {
                LOGGER.error("No record to update for key '{}'", key);
                return false;
            } else {
                LOGGER.trace("Change:  {}", tariff);

                // Price increase
                tariff.setRatePerMinute(1 + tariff.getRatePerMinute());

                this.tariffRepository.save(tariff);
                LOGGER.trace("Changed: {}", tariff);
                return true;
            }

        } catch (Exception e) {
            LOGGER.error(key, e);
            return false;
        }
    }
}
