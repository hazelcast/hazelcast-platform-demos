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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
@SpringBootApplication
@EnableConfigurationProperties(MyProperties.class)
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    /**
     * <p>Start Jet with specific configuration, and leave it running.
     * </p>
     */
    public static void main(String[] args) throws Exception {

        // Use for importing security config per environment
        String environment = System.getProperty("my.environment");
        if (environment == null || environment.length() == 0) {
            LOGGER.error("Usage: 'my.environment' system property not set");
            System.exit(1);
        }

        // Use for username/password credentials. See Dockerfile
        String password = System.getProperty("my.password");
        if (password == null || password.length() == 0) {
            LOGGER.error("Usage: 'my.password' system property not set");
            System.exit(1);
        }

        SpringApplication.run(Application.class, args);
        System.exit(0);
    }

}
