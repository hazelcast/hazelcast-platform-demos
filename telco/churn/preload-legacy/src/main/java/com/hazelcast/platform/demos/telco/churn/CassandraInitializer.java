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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecord;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordRepository;
import com.hazelcast.platform.demos.telco.churn.testdata.CustomerTestdata;

/**
 * <p>Insert data records into Cassandra, average 1000 per customer.</p>
 */
@Configuration
@EnableCassandraRepositories(basePackageClasses = CallDataRecordRepository.class)
public class CassandraInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraInitializer.class);

    // Each caller makes between 900 and 1100 calls.
    private static final int AVERAGE_CALLS = 1000;
    private static final int AVERAGE_VARIANCE = 100;

    private static final int TEN = 10;
    private static final int TWENTY_FOUR_HOURS = 24;
    private static final int SIXTY_MINUTES_OR_SECONDS = 60;

    @Autowired
    private CallDataRecordRepository callDataRecordRepository;
    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * <p>For each account, generate a block of call history.
     * These are all retrospective. The "{@code data-feed}"
     * module adds calls with today's date.
     * </p>
     */
    @Override
    public void run(String... args) throws Exception {
        long now = System.currentTimeMillis();
        long before = this.callDataRecordRepository.count();
        LOGGER.info("BEFORE: count()=={}", before);

        int accounts = CustomerTestdata.getFirstNames().length
                * CustomerTestdata.getLastNames().length;
        int insert = 0;

        for (int account = 0 ; account < accounts ; account++) {
            insert += this.insertCdrsForAccount(account, accounts, now, LocalDate.now());
        }

        long after = this.callDataRecordRepository.count();
        if ((before + insert) == after) {
            LOGGER.debug("AFTER:  count()=={}", after);
        } else {
            LOGGER.info("AFTER:  count()=={}, but {} inserts", after, insert);
        }
    }

    /**
     * <p>Generate between 900 and 1100 calls for each account,
     * using pseudo-random callers. The numbers look random but
     * are deterministic, so know what data to expect on output.
     * </p>
     */
    private int insertCdrsForAccount(int account, int accounts, long timestamp, LocalDate today) {
        int plusMinus = (account % 2 == 0) ? 1 : -1;
        int range = (account * AVERAGE_VARIANCE) / AVERAGE_VARIANCE;
        int calls = AVERAGE_CALLS + plusMinus * range;
        int insert = 0;
        String myTelno = MyUtils.getTelno(account);

        if (account % TEN == 0) {
            LOGGER.debug("Account '{}', generating {} calls", account, calls);
        }

        for (int call = 0 ; call < calls ; call++) {

            CallDataRecord cdr = new CallDataRecord();

            cdr.setCallerTelno(myTelno);
            cdr.setCallerMastId(MyUtils.getMastId(account, call));

            // Call to a pseudo-random number beyond the range of accounts, so not calling self
            int callee = account + accounts + call;
            cdr.setCalleeTelno(MyUtils.getTelno(callee));
            cdr.setCalleeMastId(MyUtils.getMastId(callee, call));

            int hour = call % TWENTY_FOUR_HOURS;
            int minute = call % SIXTY_MINUTES_OR_SECONDS;
            int second = call % SIXTY_MINUTES_OR_SECONDS;
            LocalDate callDate = today.minusDays(call);
            LocalDateTime callDateTime = LocalDateTime.of(callDate.getYear(),
                    callDate.getMonthValue(), callDate.getDayOfMonth(),
                    hour, minute, second);
            long when = callDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();

            cdr.setStartTimestamp(when);

            UUID uuid = new UUID(Long.valueOf(account), Long.valueOf(when));
            cdr.setId(uuid.toString());

            // Fake every 10th call as unsuccessful.
            if (call % TEN == 0) {
                cdr.setDurationSeconds(0);
                cdr.setCallSuccessful(false);
            } else {
                cdr.setDurationSeconds(TEN + call);
                cdr.setCallSuccessful(true);
            }

            cdr.setCreatedBy(this.springApplicationName);
            cdr.setCreatedDate(timestamp);
            cdr.setLastModifiedBy(this.springApplicationName);
            cdr.setLastModifiedDate(timestamp);

            this.callDataRecordRepository.save(cdr);
            insert++;
        }

        return insert;
    }

}
