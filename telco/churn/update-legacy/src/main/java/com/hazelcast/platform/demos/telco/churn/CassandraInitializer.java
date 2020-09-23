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

/**
 * XXX
 */
@Configuration
@EnableCassandraRepositories(basePackageClasses = CPostcodeRepository.class)
public class CassandraInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraInitializer.class);

    @Autowired
    private CPostcodeRepository postcodeRepository;

    /**
     * XXX
     */
    @Override
    public void run(String... args) throws Exception {
        List<CPostcode> list = this.postcodeRepository.findAll();

        LOGGER.error("CASSANDRA {}", list);
    }

}
