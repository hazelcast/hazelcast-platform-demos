/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecord;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordKeyProjection;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordRepository;

/**
 * <p>Update data in Cassandra, can be run multiple times</p>
 */
@Configuration
@EnableCassandraRepositories(basePackageClasses = CallDataRecordRepository.class)
public class CassandraUpdater implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraUpdater.class);

    // 2^3
    private static final int MASK_8 = 0x0008;
    private static final int SECONDS_IN_A_MINUTE = 60;
    // Cassandra 3 outputs CDC on volume threshold. Cassandra 4 uses time threshold also.
    private static final int LOOPS_MAX = 2;

    @Autowired
    private CallDataRecordRepository callDataRecordRepository;
    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * <p>Update alternate blocks of eight records</p>
     */
    @Override
    public void run(String... args) throws Exception {
        LOGGER.debug("BEFORE: count()=={}", this.callDataRecordRepository.count());

        List<CallDataRecordKeyProjection> resultsProjection =
                this.callDataRecordRepository.findByIdGreaterThan("");

        // stream.sorted() not ideal for scaling, but TreeSet not much better
        SortedSet<String> keysTmp = (SortedSet<String>) resultsProjection
                .stream()
                .map(CallDataRecordKeyProjection::getId)
                .collect(Collectors.toCollection(TreeSet::new));
        List<String> keys = new ArrayList<>(keysTmp);

        int count = 0;
        int loop = 0;
        while (loop++ < LOOPS_MAX) {
            LOGGER.info("Loop {}", loop);
            for (int i = 0 ; i < keys.size() ; i++) {
                if ((i & MASK_8) == 0) {
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

        LOGGER.debug("AFTER:  count()=={}", this.callDataRecordRepository.count());
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
            CallDataRecord callDataRecord =
                    this.callDataRecordRepository.findById(key).get();

            if (callDataRecord == null) {
                LOGGER.error("No record to update for key '{}'", key);
                return false;
            } else {
                LOGGER.trace("Change:  {}", callDataRecord);

                // Possibly tweak call success, otherwise duration
                if (key.endsWith("0")) {
                    callDataRecord.setCallSuccessful(!callDataRecord.getCallSuccessful());
                } else {
                    callDataRecord.setDurationSeconds(callDataRecord.getDurationSeconds()
                            + SECONDS_IN_A_MINUTE);
                }
                callDataRecord.setLastModifiedBy(this.springApplicationName);
                callDataRecord.setLastModifiedDate(System.currentTimeMillis());

                this.callDataRecordRepository.save(callDataRecord);
                LOGGER.trace("Changed: {}", callDataRecord);
                return true;
            }

        } catch (Exception e) {
            LOGGER.error(key, e);
            return false;
        }
    }
}
