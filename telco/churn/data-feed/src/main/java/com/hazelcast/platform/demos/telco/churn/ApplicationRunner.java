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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecord;
import com.hazelcast.platform.demos.telco.churn.testdata.CustomerTestdata;

/**
 * <p>Every second, a different customer attempts a call, and some calls are dropped.
 * </p>
 */
@Configuration
public class ApplicationRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final long LOG_THRESHOLD = 60L;
    private static final int FIVE = 5;
    private static final int TEN = 10;
    private static final long MILLIS_TO_SECONDS = 1000L;

    private static AtomicLong onSuccessCount = new AtomicLong(0);
    private static AtomicLong onFailureCount = new AtomicLong(0);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.application.name}")
    private String springApplicationName;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * <p>Create a CDR for a customer, then loop round the next customer,
     * forever.
     * </p>
     */
    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("-=-=-=-=- START '{}' START -=-=-=-=-=-", this.springApplicationName);

        int accounts = CustomerTestdata.getFirstNames().length
                * CustomerTestdata.getLastNames().length;

        LOGGER.info("Producing one call record per second");

        int insert = 0;
        long reportEvery = 1;

        // create some CDRs, wait, repeat
        try {
            while (true) {
                int account = insert % accounts;
                long now = System.currentTimeMillis();

                CallDataRecord cdr = makeCallDataRecord(account, accounts, now, insert);

                // Report 0, 2, 4, 8, 16... until every 60
                if (insert == 0 || insert % reportEvery == 0) {
                    if (reportEvery < LOG_THRESHOLD) {
                        reportEvery += reportEvery;
                    }
                    this.write(cdr, insert, true);
                } else {
                    this.write(cdr, insert, false);
                }

                insert++;
                TimeUnit.SECONDS.sleep(1L);
            }
        } catch (InterruptedException exception) {
            long good = onSuccessCount.get();
            long bad = onFailureCount.get();
            if (bad != 0) {
                LOGGER.error("Interrupted, {} bad from {}", bad, insert);
            } else {
                if ((bad + good) != insert) {
                    // Could be some in-flight
                    LOGGER.warn("Interrupted, {} bad {} good, from {} inserts",
                            bad, good, insert);
                } else {
                    LOGGER.info("Interrupted, {} inserts", insert);
                }
            }

        }

        LOGGER.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-", this.springApplicationName);
    }


    /**
     * <p>Create a "<i>random</i>" (but actually deterministic) call.
     * </p>
     *
     * @param account Whose call
     * @param accounts
     * @param now When the CDR is recorded, after when it ended
     * @param call Sequence number for calls generated
     * @return
     */
    private CallDataRecord makeCallDataRecord(int account, int accounts, long now, int call) {
        CallDataRecord cdr = new CallDataRecord();

        String myTelno = MyUtils.getTelno(account);
        cdr.setCallerTelno(myTelno);
        cdr.setCallerMastId(MyUtils.getMastId(account, call));

        // Call to a pseudo-random number beyond the range of accounts, so not calling self
        int callee = account + accounts + call;
        cdr.setCalleeTelno(MyUtils.getTelno(callee));
        cdr.setCalleeMastId(MyUtils.getMastId(callee, call));

        // Fake every 5th call as unsuccessful.
        if (call % FIVE == 0) {
            cdr.setDurationSeconds(0);
            cdr.setCallSuccessful(false);
        } else {
            cdr.setDurationSeconds(TEN + call);
            cdr.setCallSuccessful(true);
        }

        // Start time is before now, based on call duration plus a lag
        long when = now - ((cdr.getDurationSeconds() + FIVE) * MILLIS_TO_SECONDS);
        cdr.setStartTimestamp(when);

        UUID uuid = new UUID(Long.valueOf(account), Long.valueOf(when));
        cdr.setId(uuid.toString());

        cdr.setCreatedBy(this.springApplicationName);
        cdr.setCreatedDate(now);
        cdr.setLastModifiedBy(this.springApplicationName);
        cdr.setLastModifiedDate(now);

        return cdr;
    }


    /**
     * <p>Write a single object to Kafka.
     * </p>
     *
     * @param callDataRecord
     * @param count
     * @param logIt
     * @throws Exception
     */
     private void write(CallDataRecord callDataRecord, long count, boolean logIt) throws Exception {
         String key = "{ \"id\": \"" + callDataRecord.getId() + "\" }";
         String value = objectMapper.writeValueAsString(callDataRecord);

         int partition = callDataRecord.getId().hashCode() % MyConstants.KAFKA_TOPIC_CALLS_PARTITIONS;
         partition = Math.abs(partition);

         if (logIt) {
             LOGGER.info("CDR {}: key '{}' partition {} JSON => '{}'",
                     count, key, partition, value);
         }

         CompletableFuture<SendResult<String, String>> sendResult =
             kafkaTemplate.sendDefault(partition, key, value);

         sendResult.whenComplete((result, ex) -> {
             if (ex == null) {
                 onSuccessCount.incrementAndGet();
             } else {
                 onFailureCount.incrementAndGet();
                 LOGGER.error("onFailure()", ex);
             }
         });
     }
}
