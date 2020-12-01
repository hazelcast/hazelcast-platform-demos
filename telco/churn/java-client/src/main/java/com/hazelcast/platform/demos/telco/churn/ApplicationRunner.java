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

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.sql.SqlResult;

/**
 * <p>This Java client is mainly a bridge between "{@code React.js}" web front
 * end and the Hazelcast grid. However, also run some data operations to
 * demonstate security and querying.
 * </p>
 */
@Configuration
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);

    @Value("${spring.application.name}")
    private String springApplicationName;
    @Autowired
    private JetInstance jetInstance;

    /**
     * <p>Starts, runs some demonstration queries, then the "{@code @Bean}"
     * ends but the Hazelcast client stays running for web front-end.
     * </p>
     * <p>Runs the GA features, then the new 4.1 features, SQL which is
     * in Beta state
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            HazelcastInstance hazelcastInstance = this.jetInstance.getHazelcastInstance();
            LOGGER.info("-=-=-=-=- START '{}' START -=-=-=-=-=-", hazelcastInstance.getName());
            this.gaFeatures(hazelcastInstance);
            LOGGER.info("-=-=-=  MIDDLE  '{}'  MIDDLE  =-=-=-=-", hazelcastInstance.getName());
            this.betaFeatures(this.jetInstance);
            LOGGER.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-", hazelcastInstance.getName());
        };
    }

    /**
     * <p>Try some standard map operations against all the maps we can find.
     * Because security is in place, some operations may be rejected.
     * </p>
     *
     * @param hazelcastInstance
     */
    private void gaFeatures(HazelcastInstance hazelcastInstance) {
        for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
            // Hide system objects
            if (!distributedObject.getName().startsWith("__")) {
                if (distributedObject instanceof IMap) {
                    IMap<?, ?> iMap = (IMap<?, ?>) distributedObject;
                    String mapName = iMap.getName();
                    try {
                        int size = iMap.size();
                        LOGGER.info("getMap({}).size()=={}", iMap.getName(), size);
                        // Pause so logger messages don't cross
                        TimeUnit.SECONDS.sleep(1L);
                    } catch (Exception e) {
                        String message = String.format("getMap(%s).size()", mapName);
                        LOGGER.error(message + ":" + e.getMessage());
                    }
                    try {
                        Set<?> keys = iMap.keySet();
                        LOGGER.info("{}.keySet().size()=={}", iMap.getName(), keys.size());
                        // Pause so logger messages don't cross
                        TimeUnit.SECONDS.sleep(1L);
                        // Sort if possible
                        if (keys.size() > 0
                                && keys.iterator().next() instanceof Comparable) {
                            keys = new TreeSet<>(keys);
                        }
                        int count = 0;
                        for (Object key : keys) {
                            count++;
                            if (count == MyConstants.SQL_RESULT_THRESHOLD) {
                                LOGGER.info("-- truncated at count {}", count);
                                break;
                            }
                            try {
                                Object value = iMap.get(key);
                                LOGGER.info("Map {}.get({})=={}", iMap.getName(), key, value);
                                // Pause so logger messages don't cross
                                TimeUnit.SECONDS.sleep(1L);
                            } catch (Exception e) {
                                String message = String.format("%s.get(%s)", mapName, key.toString());
                                LOGGER.error(message + ":" + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        String message = String.format("getMap(%s).keySet()", mapName);
                        LOGGER.error(message + ":" + e.getMessage());
                    }
                } else {
                    LOGGER.info("distributedObject '{}' !instanceof IMap, is {}",
                        distributedObject.getName(), distributedObject.getClass().getName());
                }
            }
        }
    }

    /**
     * <p>Demonstrate beta features in Hazelcast IMDG 4.1 and Jet 4.4, the new
     * SQL. As these are beta features, they may change in 4.2 and beyond until GA.
     * </p>
     *
     * @param jetInstance
     */
    private void betaFeatures(JetInstance jetInstance) {
        String[] queries = new String[] {
                // IMap with Portable
                "SELECT * FROM " + MyConstants.IMAP_NAME_SENTIMENT,
                // Above with function, need to escape as current is reserved word
                "SELECT __key, FLOOR(\"current\") || '%' AS \"Churn Risk\""
                        + " FROM " + MyConstants.IMAP_NAME_SENTIMENT,
                        // ORDER BY not yet supported
                        //+ " ORDER BY \"current\" DESC"
                // Kafka topic with JSON
                "SELECT * FROM " + MyConstants.KAFKA_TOPIC_CALLS_NAME,
                // Above by with projection, selection
                "SELECT id, callerTelno, calleeTelno, callSuccessful"
                        + " FROM " + MyConstants.KAFKA_TOPIC_CALLS_NAME
                        + " WHERE durationSeconds > 0"
        };

        int count = 0;
        for (String query : queries) {
            query = MyUtils.makeUTF8(query);
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("");
                count++;
                System.out.printf("(%d)%n", count);
                System.out.println(query);
                SqlResult sqlResult = jetInstance.getSql().execute(query);
                System.out.println(MyUtils.prettyPrintSqlResult(sqlResult));
                System.out.println("");
            } catch (Exception e) {
                String message = String.format("SQL '%s'", query);
                LOGGER.error(message + ": " + e.getMessage());
            }
        }
    }

}
