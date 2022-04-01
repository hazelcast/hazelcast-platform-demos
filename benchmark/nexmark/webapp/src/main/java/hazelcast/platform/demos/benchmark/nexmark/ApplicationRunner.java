/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.benchmark.nexmark;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Mainly leave it up to React.js
 * </p>
 */
@Configuration
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);

    @Value("${spring.application.name}")
    private String springApplicationName;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            LOGGER.info("-=-=-=-=- '{}' START -=-=-=-=-=-", this.springApplicationName);

            try {
                while (true) {
                    TimeUnit.MINUTES.sleep(1L);
                    LOGGER.info("-=-=-=-=- '{}' RUNNING -=-=-=-=-=-", this.springApplicationName);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted: {}", e.getMessage());
            }

            LOGGER.info("-=-=-=-=- '{}' END -=-=-=-=-=-", this.springApplicationName);
        };
    }

}
