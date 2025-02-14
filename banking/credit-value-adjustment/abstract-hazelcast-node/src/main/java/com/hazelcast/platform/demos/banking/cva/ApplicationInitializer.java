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

package com.hazelcast.platform.demos.banking.cva;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;

/**
 * <p>Ensure the server is in a ready state, by requesting all the
 * set-up processing runs. This is idempotent. All servers will request
 * but only the <i>n</i>th will result in anything happening.
 * </p>
 */
@Configuration
public class ApplicationInitializer {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * <p>Use a Spring "{@code @Bean}" to kick off the necessary
     * initialisation after the objects we need are ready.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
       return args -> {
           CVAIdempotentInitialization.fullInitialize(this.hazelcastInstance);
       };
    }

}
