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

package com.hazelcast.platform.demos.telco.churn;

import java.security.AccessControlException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.map.IMap;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.topic.ITopic;

/**
 * <p>This Java client is mainly a bridge between "{@code React.js}" web front
 * end and the Hazelcast grid. However, also run some data operations to
 * demonstrate security and querying.
 * </p>
 */
@Configuration
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final long THIRTY = 30L;

    private static final String DESTINATION =
            "/" + MyLocalConstants.WEBSOCKET_TOPICS_PREFIX
            + "/" + MyLocalConstants.WEBSOCKET_SIZES_SUFFIX;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MySocketTopicListener mySocketTopicListener;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Value("${spring.application.name}")
    private String springApplicationName;

    /**
     * <p>Starts, runs some demonstration queries, then the "{@code @Bean}"
     * ends but the Hazelcast client stays running for web front-end.
     * </p>
     * <p>Runs the GA features, then the new 4.1 features, SQL which is
     * in Beta state
     * </p>
     * <p>Finally, starts publishing map sizes to a web socket to push
     * these to the web client's "{@code React}" front-end.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
             this.addTopicLister(this.hazelcastInstance);

            LOGGER.info("-=-=-=-=- START '{}' START -=-=-=-=-=-", hazelcastInstance.getName());
            this.gaFeatures(hazelcastInstance);
            LOGGER.info("-=-=-=  MIDDLE  '{}'  MIDDLE  =-=-=-=-", hazelcastInstance.getName());
            this.betaFeatures(this.hazelcastInstance);
            LOGGER.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-", hazelcastInstance.getName());

            LOGGER.debug("Starting web-socket feed");
            this.webSocketFeed();
        };
    }

    /**
     * <p>Add a topic listener. If the topic doesn't exist, accessing
     * it will try to create, which will fail if we don't have create
     * permission. This would mean the client is connecting before
     * the server side has done its initialization.
     * </p>
     */
    private void addTopicLister(HazelcastInstance hazelcastInstance) {
        try {
            ITopic<String> slackTopic =
                    hazelcastInstance.getTopic(MyConstants.ITOPIC_NAME_SLACK);
            // Listen for "slack" topic messages to a web socket
            slackTopic.addMessageListener(this.mySocketTopicListener);
        } catch (AccessControlException e) {
            String message = String.format("Topic '%s' probably doesn't yet exist,"
                    + " client started before server initialization",
                    MyConstants.ITOPIC_NAME_SLACK);
            LOGGER.error(message + ":" + e.getMessage());
        }
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
     * @param hazelcastInstance
     */
    private void betaFeatures(HazelcastInstance hazelcastInstance) {
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
                SqlResult sqlResult = this.hazelcastInstance.getSql().execute(query);
                Tuple3<String, String, List<String>> result =
                        MyUtils.prettyPrintSqlResult(sqlResult);
                if (result.f0().length() > 0) {
                    // Error
                    System.out.println(result.f0());
                } else {
                    // Actual data
                    result.f2().stream().forEach(System.out::println);
                    if (result.f1().length() > 0) {
                        // Warning
                        System.out.println(result.f1());
                    }
                }
                System.out.println("");
            } catch (Exception e) {
                String message = String.format("SQL '%s'", query);
                LOGGER.error(message + ": " + e.getMessage());
            }
        }
    }

    /**
     * <p>Periodically send data to a web socket for the
     * "{@code React}" front-end to receive.
     * </p>
     * <p>We do it this way to only send info that has changed,
     * rather than have the web socket polling.
     * </p>
     */
    private void webSocketFeed() {
        Set<String> mapNames = new TreeSet<>(MyConstants.IMAP_NAMES);
        Map<String, Integer> previousMapSizes =
                mapNames
                .stream()
                .map(mapName -> {
                    try {
                        return Tuple2.tuple2(mapName, this.hazelcastInstance.getMap(mapName).size());
                    } catch (Exception e) {
                        return Tuple2.tuple2(mapName, Integer.valueOf(-1));
                    }
                })
                .collect(Collectors.toMap(Tuple2::f0, Tuple2::f1));

        // Loop, not too aggressively
        while (true) {
            try {
                // Checkstyle thinks the below is more obvious than "TimeUnit.SECONDS.sleep(30)"
                TimeUnit.SECONDS.sleep(THIRTY);

                // Find changes
                Map<String, Integer> currentMapSizes =
                        previousMapSizes
                        .entrySet()
                        .stream()
                        // Remove "-1". permission denied, no point retrying
                        .filter(entry -> entry.getValue() >= 0)
                        .map(entry -> Tuple3.tuple3(entry.getKey(), entry.getValue(),
                                    this.hazelcastInstance.getMap(entry.getKey()).size()))
                        // Remove if size unchanged
                        .filter(tuple3 -> tuple3.f1() != tuple3.f2())
                        .collect(Collectors.toMap(Tuple3::f0, Tuple3::f2));

                // Anything different ?
                if (currentMapSizes.size() != 0) {
                    // Save changes
                    previousMapSizes.putAll(currentMapSizes);

                    // Send to React
                    this.webSocketSend(currentMapSizes);
                }

            } catch (InterruptedException e) {
                break;
            }
        }

    }

    /**
     * <p>Format as JSON and send to web socket.
     * </p>
     *
     * @param currentMapSizes Only those that have changed
     */
    private void webSocketSend(Map<String, Integer> currentMapSizes) {
        long now = System.currentTimeMillis();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"sizes\": [");

        // Alphabetical order, handling security error
        int count = 0;
        for (Entry<String, Integer> entry : currentMapSizes.entrySet()) {
            stringBuilder.append("{ \"name\": \"" + entry.getKey() + "\",");
            stringBuilder.append("\"size\": " + entry.getValue() + " }");
            count++;
            if (count < currentMapSizes.size()) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append("], \"now\": \"" + now + "\"");
        stringBuilder.append(" }");

        String result = stringBuilder.toString();
        // Trace not debug as map size changing is normal
        LOGGER.trace("Sending to websocket '{}'", result);

        this.simpMessagingTemplate.convertAndSend(DESTINATION, result);
    }
}
