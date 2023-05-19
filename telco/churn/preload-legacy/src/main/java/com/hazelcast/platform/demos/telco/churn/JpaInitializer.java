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

package com.hazelcast.platform.demos.telco.churn;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.Tariff;
import com.hazelcast.platform.demos.telco.churn.domain.TariffRepository;
import com.hazelcast.platform.demos.telco.churn.testdata.TariffTestdata;

/**
 * <p>Inserts tariffs into the table for the current and next calendar year.
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = TariffRepository.class)
public class JpaInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaInitializer.class);

    @Autowired
    private TariffRepository tariffRepository;

    /**
     * Inject data if table empty.
     */
    @Override
    public void run(String... args) throws Exception {
        long before = this.tariffRepository.count();
        LOGGER.info("BEFORE: count()=={}", before);

        int year = LocalDate.now().getYear();

        List<String> listNow = this.tariffRepository.findThisYearsTariffs(year);
        List<String> listFuture = this.tariffRepository.findThisYearsTariffs(year + 1);

        int insert = 0;
        if ((listNow.size() + listFuture.size()) != 0) {
            LOGGER.error("Tariffs already loaded, ignoring");
        } else {
            insert = this.saveTariffs(year);
        }

        long after = this.tariffRepository.count();
        if ((before + insert) == after) {
            LOGGER.info("AFTER:  count()=={}", after);
        } else {
            LOGGER.warn("AFTER:  count()=={}, but {} inserts", after, insert);
        }
    }

    /**
     * <p>Allow exceptions if bad data, fail fast.
     * </p>
     *
     * @param year Base for offset.
     */
    private int saveTariffs(int currentYear) {
        int count = 0;
        Set<Integer> years = new TreeSet<>();

        for (Object[] tariffData : TariffTestdata.getTariffs()) {
            Tariff tariff = new Tariff();
            Iterator<Object> iterator = Arrays.asList(tariffData).iterator();

            // Base year from runtime plus offset from test data
            int effectiveYear = currentYear + Integer.parseInt(iterator.next().toString());
            years.add(effectiveYear);

            tariff.setYear(effectiveYear);
            tariff.setId(iterator.next().toString() + effectiveYear);
            tariff.setName(iterator.next().toString() + " " + effectiveYear);
            tariff.setInternational(Boolean.parseBoolean(iterator.next().toString()));
            tariff.setRatePerMinute(Double.parseDouble(iterator.next().toString()));

            this.tariffRepository.save(tariff);
            LOGGER.trace("saved: {}", tariff);
            count++;
        }

        LOGGER.info("Wrote {} records for years {}", count, years);
        return count;
    }

}
