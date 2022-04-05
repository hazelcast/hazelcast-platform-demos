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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;

/**
 * <p>Mainly leave it up to React.js
 * </p>
 */
@Configuration
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Value("${spring.application.name}")
    private String springApplicationName;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            LOGGER.info("-=-=-=-=- '{}' START -=-=-=-=-=-", this.springApplicationName);

            boolean ok = this.init();

            if (ok) {
                try {
                    while (true) {
                        TimeUnit.MINUTES.sleep(1L);
                        LOGGER.info("-=-=-=-=- '{}' RUNNING -=-=-=-=-=-", this.springApplicationName);
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn("Interrupted: {}", e.getMessage());
                }
            }

            LOGGER.info("-=-=-=-=- '{}' END -=-=-=-=-=-", this.springApplicationName);
        };
    }

    /**
     * <p>Define mappings
     * </p>
     * @return
     */
    private boolean init() {
        String definitionBody1 = "("
                + "    __key VARCHAR,"
                + "    \"timestamp\" BIGINT,"
                + "    timestamp_str VARCHAR,"
                + "    latency_ms BIGINT,"
                + "    offset_ms BIGINT"
                + ")"
                 + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";

        String definitionBody2 = "("
                + "    __key VARCHAR,"
                + "    \"timestamp\" BIGINT,"
                + "    timestamp_str VARCHAR,"
                + "    latency_ms BIGINT,"
                + "    offset_ms BIGINT"
                + ")"
                 + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";

        boolean ok = this.define(BenchmarkBase.IMAP_NAME_CURRENT_LATENCIES, definitionBody1);
        ok = ok & this.define(BenchmarkBase.IMAP_NAME_MAX_LATENCIES, definitionBody2);
        return ok;
    }

    private boolean define(String mapName, String definitionBody) {
        String definition = "CREATE OR REPLACE MAPPING " + mapName + " " + definitionBody;
        LOGGER.debug("Definition '{}'", definition);
        try {
            // Ensure definition and object both exist
            this.hazelcastInstance.getSql().execute(definition);
            this.hazelcastInstance.getMap(mapName);
            return true;
        } catch (Exception e) {
            LOGGER.error(definition, e);
            return false;
        }
    }
}
