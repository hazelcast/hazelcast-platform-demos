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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecord;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordRepository;

/**
 * XXX
 */
@Configuration
@EnableCassandraRepositories(basePackageClasses = CallDataRecordRepository.class)
public class CassandraInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraInitializer.class);

    @Autowired
    private CallDataRecordRepository callDataRecordRepository;

    /**
     * XXX
     */
    @Override
    public void run(String... args) throws Exception {
        List<CallDataRecord> list = this.callDataRecordRepository.findAll();
        LOGGER.error("CASSANDRA BEFORE {}", list);

        CallDataRecord cdr0 = new CallDataRecord();
        cdr0.setId("a");
        cdr0.setCallerTelno("a1");
        cdr0.setCallerMastId("a2");
        cdr0.setCalleeTelno("a3");
        cdr0.setCalleeMastId("a4");
        cdr0.setDurationSeconds(1);
        cdr0.setStartTimestamp(Long.MIN_VALUE);
        cdr0.setSuccessful(false);
        this.callDataRecordRepository.save(cdr0);
        CallDataRecord cdr1 = new CallDataRecord();
        cdr1.setId("b");
        cdr1.setCallerTelno("b1");
        cdr1.setCallerMastId("b2");
        cdr1.setCalleeTelno("b3");
        cdr1.setCalleeMastId("b4");
        cdr1.setDurationSeconds(2);
        cdr1.setStartTimestamp(Long.MAX_VALUE);
        cdr1.setSuccessful(true);
        this.callDataRecordRepository.save(cdr1);

        list = this.callDataRecordRepository.findAll();
        LOGGER.error("CASSANDRA AFTER {}", list);
    }

}
