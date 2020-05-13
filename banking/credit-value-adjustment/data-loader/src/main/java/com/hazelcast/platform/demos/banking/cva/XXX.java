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

package com.hazelcast.platform.demos.banking.cva;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.banking.cva.cvastp.CvaStpJobSubmitter;

/**
 * XXX Temporary job submit
 */
@Component
@Order(value = 5)
public class XXX implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(XXX.class);

    //private static final int ALL_TEN_THOUSAND = 10000;
    private static final int MAX_DUPLICATES_TO_LOG = 10;
    private static final int PERIODIC_PROGRESS_INTERVAL = 50_000;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private JetInstance jetInstance;

    private int duplicates;
    private int errors;
    private int read;
    private boolean stopEarly;
    private int threshold;
    private int written;

    @Override
    public void run(String... args) throws Exception {
        //String inputFileName = "mtms.json";
        //String keyFieldName1 = "tradeid";
        //String keyFieldName2 = "curvename";
        //this.load("mtms", inputFileName, keyFieldName1, keyFieldName2);
        //XXX Temp coding
        LOGGER.info("cva()");

        boolean debug = false;

        try {
            LocalDate calcDate = /*LocalDate.of(2016, 01, 07);*/ LocalDate.parse("2016-01-07");
            Job job = CvaStpJobSubmitter.submitCvaStpJob(this.jetInstance, calcDate, debug);
            TimeUnit.SECONDS.sleep(1);
            System.out.println("");
            System.out.println(new JSONObject(this.logJob(job)));
            System.out.println("");
            TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                LOGGER.info("cva()", e);
                System.out.println("{"
                    + " \"exception_class\": \"" + e.getClass().getName() + "\""
                    + ", \"exception_message\": \"" + e.getMessage() + "\""
                    + " }");
            }
        }

    private String logJob(Job job) {
        try {
            LOGGER.info("{}", job);
            return "{"
                    + " \"id\": \"" + job.getId() + "\""
                    + ", \"name\": \"" + job.getName() + "\""
                    + ", \"status\": \"" + job.getStatus() + "\""
                    + ", \"submission_time\": \"" + job.getSubmissionTime() + "\""
                    + ", \"submission_time_str\": \"" + new Date(job.getSubmissionTime()) + "\""
                    + " }";
        } catch (Exception e) {
            LOGGER.info(job.toString(), e);
            return "{"
                    + " \"exception_class\": \"" + e.getClass().getName() + "\""
                    + ", \"exception_message\": \"" + e.getMessage() + "\""
                    + " }";
        }
    }

    public boolean load(String mapName, String inputFileName, String keyFieldName1, String keyFieldName2) {
        IMap<Tuple2<String, String>, HazelcastJsonValue> iMap = this.hazelcastInstance.getMap(mapName);

        this.duplicates = 0;
        this.errors = 0;
        this.read = 0;
        this.stopEarly = false;
        this.written = 0;

        Resource resource = this.applicationContext.getResource("classpath:" + inputFileName);

        this.handleNormalFile(resource, inputFileName, keyFieldName1, keyFieldName2, iMap);

        if (this.duplicates == 0 && this.errors == 0) {
            LOGGER.info("File '{}', read {} lines, wrote {} entries into map '{}'",
                    inputFileName, this.read, this.written, iMap.getName());
        } else {
            LOGGER.warn("File '{}', read {} lines, wrote {} entries into map '{}', {} duplicates {} errors",
                    inputFileName, this.read, this.written, iMap.getName(),
                    this.duplicates, this.errors);
        }
        return (this.errors == 0);
    }

    private void handleNormalFile(Resource resource, String inputFileName, String keyFieldName1, String keyFieldName2,
            IMap<Tuple2<String, String>, HazelcastJsonValue> iMap) {
        try (BufferedReader bufferedReader =
                new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = bufferedReader.readLine();
            if (line != null) {
                this.read++;
            }
            while (!this.stopEarly && line != null) {
                if (!line.startsWith("#")) {
                    try {
                        HazelcastJsonValue value = new HazelcastJsonValue(line);

                        // Needed until https://github.com/hazelcast/hazelcast/issues/15140
                        JSONObject json = new JSONObject(value.toString());
                        Tuple2<String, String> key =
                                Tuple2.tuple2(json.get(keyFieldName1).toString(), json.get(keyFieldName2).toString());

                        this.testDuplicate(iMap, key, line);
                        this.tryWrite(iMap, key, value);

                    } catch (Exception exception) {
                        this.errors++;
                        LOGGER.error("Line {} of {}: '{}' for '{}'", this.read, inputFileName,
                                exception.getMessage(), line);
                    }
                }
                if (!this.stopEarly) {
                    line = bufferedReader.readLine();
                    if (line != null) {
                        this.read++;
                    }
                    if (this.read % PERIODIC_PROGRESS_INTERVAL == 0) {
                        LOGGER.debug("Line {} of {} ... processing", this.read, inputFileName);
                    }
                }
            }
        } catch (IOException e) {
            this.errors++;
            LOGGER.debug("Processing line {} of {} ...", this.read, inputFileName);
        }
    }

    private void tryWrite(IMap<Tuple2<String, String>, HazelcastJsonValue> iMap,
            Tuple2<String, String> key, HazelcastJsonValue value) {
        if (this.threshold > 0) {
            if (this.written < this.threshold) {
                iMap.set(key, value);
                this.written++;
            }
            if (this.written == this.threshold) {
                this.stopEarly = true;
            }
        } else {
            iMap.set(key, value);
            this.written++;
        }
    }

    private void testDuplicate(IMap<Tuple2<String, String>, HazelcastJsonValue> iMap, Tuple2<String, String> key, String line) {
        if (iMap.containsKey(key)) {
            this.duplicates++;
            if (this.duplicates <= MAX_DUPLICATES_TO_LOG) {
                if (line.length() > MyConstants.HALF_SCREEN_WIDTH) {
                    LOGGER.warn("Duplicate key '{}' on '{} ......", key,
                            line.substring(0, MyConstants.HALF_SCREEN_WIDTH));
                } else {
                    LOGGER.warn("Duplicate key '{}' on '{}'", key, line);
                }
            }
        }
    }

}
