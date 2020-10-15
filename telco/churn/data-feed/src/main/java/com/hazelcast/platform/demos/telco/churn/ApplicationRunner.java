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

import java.util.concurrent.TimeUnit;
import java.util.UUID;
//XXX import java.util.Arrays;
//XXX import java.util.List;
//XXX import java.util.concurrent.CountDownLatch;
//XXX import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * XXX
 */
@Configuration
public class ApplicationRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int INDENT = 4;
    private static final long LOG_THRESHOLD = 10_000L;
    private static final int MAX_BATCH_SIZE = 16 * 1024;

    //XXX private static CountDownLatch countDownLatch;
    private static AtomicLong onSuccessCount = new AtomicLong(0);
    private static AtomicLong onFailureCount = new AtomicLong(0);

    private final MyCallback myCallback = new MyCallback();

    private int batchSize = DEFAULT_BATCH_SIZE;

    @Value("${spring.application.name}")
    private String springApplicationName;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * <p>Produce a batch of 100 CDRs at once, then wait a second and
     * repeat. For an alternative approach, to produce these evenly
     * spread throughout the second interval, see the {@code Trade Monitor}
     * <a href="https://github.com/hazelcast/hazelcast-platform-demos/blob/
master/banking/trade-monitor/trade-producer/
src/main/java/com/hazelcast/platform/demos/banking/trademonitor/ApplicationRunner.java#L97">here</a>
     * which will produce one record every .00333 seconds to output 300/second.
     * </p>
     */
    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("-=-=-=-=- START {} START -=-=-=-=-=-", this.springApplicationName);

        this.setBatchSize(args);
        LOGGER.info("Producing {} call records in a batch per second", this.batchSize);

        long count = 0;
        long reportEvery = 1;

        // create some CDRs, wait, repeat
        try {
            while (true) {
                for (int i = 0; i < this.batchSize; i++) {
                    //FIXME CDR
                    String id = UUID.randomUUID().toString();
                    String value = "{account: \"" + i + "\", id: \"" + id + "\"}";

                    // Report 0, 2, 4, 8, 16... until every 10,000
                    if (count == 0 || count % reportEvery == 0) {
                        if (reportEvery < LOG_THRESHOLD) {
                            reportEvery += reportEvery;
                        }
                        this.write(value, count, true);
                    } else {
                        this.write(value, count, false);
                    }
                    count++;
                }

                TimeUnit.SECONDS.sleep(1L);
            }
        } catch (InterruptedException exception) {
        }

        LOGGER.info("-=-=-=-=-  END  {}  END  -=-=-=-=-=-", this.springApplicationName);
    }

    /**
     * <p>Possibly change batch size with command line argument.
     * </p>
     *
     * @param args
     */
    private void setBatchSize(String... args) {
        if (args.length > 0) {
            try {
                int i = Integer.parseInt(args[0]);
                if (i > 0) {
                    if (i == DEFAULT_BATCH_SIZE) {
                        LOGGER.warn("Supplied batch size {} same as default", i);
                    } else {
                        if (i < MAX_BATCH_SIZE) {
                            LOGGER.info("Using {} from args as batch size", i);
                            this.batchSize = i;
                        } else {
                            LOGGER.warn("Supplied batch size {} > max allowed {}", i, MAX_BATCH_SIZE);
                        }
                    }
                } else {
                    LOGGER.error("Ignoring {} from args as batch size", i);
                }
                return;
            } catch (NumberFormatException nfe) {
                LOGGER.error("Arg '{}' provided for batch size", args[0]);
            }
        }
    }


    /**
     * <p>Write a single object to Kafka.
     * </p>
     *
     * @param value
     * @param count
     * @param logIt
     * @throws Exception
     */
     private void write(String value, long count, boolean logIt) throws Exception {
         JSONObject jsonObject = new JSONObject(value);
         String key = jsonObject.getString("account");

         int partition = key.hashCode() % MyConstants.KAFKA_TOPIC_CALLS_PARTITIONS;

         if (logIt) {
             LOGGER.info("CDR {}: key '{}' partition {} JSON => '{}'",
                     count, key, partition, jsonObject.toString(INDENT));
         }

         ListenableFuture<SendResult<String, String>> sendResult =
             kafkaTemplate.sendDefault(partition, key, value);

         sendResult.addCallback(this.myCallback);
     }

     /**
      * <p>A callback to count successful and unsuccessful writes to Kafka.
      * </p>
      */
     private static class MyCallback implements ListenableFutureCallback<SendResult<String, String>> {

         @Override
         public void onSuccess(SendResult<String, String> result) {
             onSuccessCount.incrementAndGet();
         }

         @Override
         public void onFailure(Throwable ex) {
             onFailureCount.incrementAndGet();
             LOGGER.error("onFailure()", ex);
         }

     }
}
