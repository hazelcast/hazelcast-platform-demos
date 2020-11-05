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
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecord;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordRepository;

/**
 * <p>Insert data records into Cassandra.</p>
 */
@Configuration
@EnableCassandraRepositories(basePackageClasses = CallDataRecordRepository.class)
public class CassandraInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraInitializer.class);
    //XXX
    private static final int XXX = 100;

    @Autowired
    private CallDataRecordRepository callDataRecordRepository;
    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * XXX
     */
    @Override
    public void run(String... args) throws Exception {
        LOGGER.debug("BEFORE: count()=={}", this.callDataRecordRepository.count());

        long now = System.currentTimeMillis();

        for (int i = 0 ; i < XXX; i++) {
            CallDataRecord cdr = new CallDataRecord();
            cdr.setId("i" + i);
            cdr.setCallerTelno("i" + i);
            cdr.setCallerMastId("i" + i);
            cdr.setCalleeTelno("i" + i);
            cdr.setCalleeMastId("i" + i);
            cdr.setDurationSeconds(0);
            cdr.setStartTimestamp(0L);
            cdr.setCallSuccessful(true);
            cdr.setCreatedBy(this.springApplicationName);
            cdr.setCreatedDate(now);
            cdr.setLastModifiedBy(this.springApplicationName);
            cdr.setLastModifiedDate(now);
            this.callDataRecordRepository.save(cdr);
        }

        LOGGER.debug("AFTER:  count()=={}", this.callDataRecordRepository.count());
    }

}
