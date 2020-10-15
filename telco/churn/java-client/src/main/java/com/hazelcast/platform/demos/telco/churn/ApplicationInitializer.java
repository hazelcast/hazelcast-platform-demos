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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
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
//FIXME import com.hazelcast.sql.SqlResult;
import com.hazelcast.topic.ITopic;

/**
 * <p>XXX
 * </p>
 */
@Configuration
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);
    private static final long TEN_MINUTES = 600L;

    @Value("${spring.application.name}")
    private String springApplicationName;
    @Autowired
    private JetInstance jetInstance;

    /**
     * <p>XXX
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            HazelcastInstance hazelcastInstance = this.jetInstance.getHazelcastInstance();
            LOGGER.info("-=-=-=-=- START {} START -=-=-=-=-=-", hazelcastInstance.getName());

            for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
                if (distributedObject instanceof IMap) {
                    IMap<?, ?> iMap = (IMap<?, ?>) distributedObject;
                    String mapName = iMap.getName();
                    if (!mapName.startsWith("__")) {
                        try {
                            int size = iMap.size();
                            LOGGER.info("getMap({}).size()=={}", iMap.getName(), size);
                        } catch (Exception e) {
                            String message = String.format("getMap(%s).size()", mapName);
                            LOGGER.error(message + ":" + e.getMessage());
                        }
                        try {
                            Set<?> keys = iMap.keySet();
                            LOGGER.info("{}.keySet().size()=={}", iMap.getName(), keys.size());
                            for (Object key : keys) {
                                try {
                                    Object value = iMap.get(key);
                                    LOGGER.info("Map {}.get({})=={}", iMap.getName(), key, value);
                                    ITopic<String> topic = hazelcastInstance.getTopic(MyConstants.ITOPIC_NAME_SLACK);
                                    String payload = "Hello";
                                    String message = this.springApplicationName
                                            + " (build: " + key + ", " + value + ")"
                                            + " '" + payload + "' @ "
                                            + MyUtils.timestampToISO8601(System.currentTimeMillis());
                                    if (iMap.getName().equals("neil")) {
                                        LOGGER.info("Topic {}.publish('{}')", topic.getName(), message);
                                        topic.publish(message);
                                    }
                                    TimeUnit.SECONDS.sleep(3L);
                                } catch (Exception e) {
                                    String message = String.format("%s.get(%s)", mapName, key.toString());
                                    LOGGER.error(message + ":" + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            String message = String.format("getMap(%s).keySet()", mapName);
                            LOGGER.error(message + ":" + e.getMessage());
                        }
                    }
                } else {
                    LOGGER.info("distributedObject '{}' !instanceof IMap, is {}",
                            distributedObject.getName(), distributedObject.getClass().getName());
                }
            }

            LOGGER.info("-=-=-=  MIDDLE  {}  MIDDLE  =-=-=-=-", hazelcastInstance.getName());
            this.beta(hazelcastInstance);
            LOGGER.info("-=-=-=-=-  END  {}  END  -=-=-=-=-=-", hazelcastInstance.getName());
            TimeUnit.SECONDS.sleep(TEN_MINUTES);
            hazelcastInstance.shutdown();
        };
    }

    /**
     * XXX Test BETA code
     * @param hazelcastInstance
     */
    private void beta(HazelcastInstance hazelcastInstance) {
        String mapName = Person.class.getSimpleName();

        Person person1 = new Person();
        Person person2 = new Person();
        Person person3 = new Person();

        person1.setFirstName("Neil");
        person1.setLastName("Stevenson");
        person2.setFirstName("Xxx");
        person2.setLastName("Stevenson");
        person3.setFirstName("Neil");
        person3.setLastName("Zzz");

        try {
            IMap<Integer, Object> personMap = hazelcastInstance.getMap(mapName);
            personMap.put(1,  person1);
            personMap.put(2,  person2);
            personMap.put(3,  person3);

            Set<Integer> keys = personMap.keySet();
            LOGGER.info("{}.keySet().size()=={}", personMap.getName(), keys.size());

            Set<Integer> targetKeys = new HashSet<>();
            targetKeys.add(1);
            targetKeys.add(3);

            CompletionStage<Map<Integer, String>> completionStage =
                    personMap.submitToKeys(targetKeys, new MyXXXEntryProcessor());

            completionStage.thenAccept(map -> {
                System.out.println((map.getClass()) + " map size ==" + map.size());
                /*XXX
                for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
                    System.out.println("Entry key1 " + entry.getKey().getClass());
                    System.out.println("Entry key2 " + entry.getKey());
                    System.out.println("Entry val1 " + entry.getValue().getClass());
                    System.out.println("Entry val2 " + entry.getValue());
                }*/
            });

        } catch (Exception e) {
            String message = String.format("(%s).submitToKeys()", mapName);
            LOGGER.error(message + ":" + e.getMessage());
        }

        //String query = "SELECT firstName FROM '" + mapName + "' WHERE lastName = 'Stevenson'";
        //String query = "SELECT firstName FROM \"" + mapName + "\" WHERE lastName = 'Stevenson'";
        //String query = "SELECT firstName FROM " + mapName + " WHERE lastName = 'Stevenson'";
        String query = "SELECT firstName FROM Person WHERE lastName = “Stevenson”";
        //String query = "SELECT firstName from Person WHERE lastName = ‘Stevenson’";
        query = MyUtils.makeUTF8(query);
        //FIXME
        LOGGER.error("Needs IMDG 4.1 for {}", query);
        /*FIXME needs IMDG 4.1
        try {
            SqlResult sqlResult = hazelcastInstance.getSql().execute(query);
            System.out.println(MyUtils.prettyPrintSqlResult(sqlResult));
        } catch (Exception e) {
            String message = String.format("getMap(%s) SQL '%s'", mapName, query);
            LOGGER.warn(message + ": " + e.getMessage());
            LOGGER.error("XXX", e);
        }*/
    }
}
