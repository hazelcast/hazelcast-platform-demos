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

package com.hazelcast.platform.demos.travel.booking;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.map.IMap;

/**
 * <p>XXX</p>
 */
@Configuration
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final int FIVE = 5;
    private static final long THIRTY = 30L;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * <p>XXX
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            LOGGER.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                    this.hazelcastInstance.getName());

            this.xxx1();
            int count = 0;
            while (count < THIRTY) {
                TimeUnit.MINUTES.sleep(1);
                count++;
                String countStr = String.format("%05d", count);
                LOGGER.info("-=-=-=-=- {} '{}' {} -=-=-=-=-=-",
                        countStr, this.hazelcastInstance.getName(), countStr);
                this.xxx2();
                if (count % FIVE == 0) {
                    this.xxx3(count);
                }
            }

            LOGGER.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-",
                    this.hazelcastInstance.getName());
        };
    }

    /**
     * <p>XXX
     * </p>
     */
    private void xxx1() {
        String name = System.getProperty("user.name");
        for (int i = 0 ; i < FIVE; i++) {
            String key = "hello" + i;
            String value = "world" + i;
            this.hazelcastInstance.getMap(name).set(key, value);
        }
    }

    /**
     * <p>XXX
     * </p>
     */
    private void xxx2() {
        Set<String> mapNames = this.hazelcastInstance
                .getDistributedObjects()
                .stream()
                .filter(distributedObject -> (distributedObject instanceof IMap))
                //FIXME .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .map(distributedObject -> distributedObject.getName())
                .collect(Collectors.toCollection(TreeSet::new));

        mapNames
        .forEach(name -> {
            IMap<?, ?> iMap = this.hazelcastInstance.getMap(name);
            LOGGER.info("MAP '{}'.size() => {}", iMap.getName(), iMap.size());
        });
    }

    /**
     * <p>XXX
     * </p>
     */
    private void xxx3(int i) {
        String name = System.getProperty("user.name");
        Pipeline pipeline = Pipeline.create();
        pipeline
        .readFrom(Sources.map(name))
        .writeTo(Sinks.logger());

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("run-" + i);

        Job job = this.hazelcastInstance.getJet().newJobIfAbsent(pipeline, jobConfig);
        LOGGER.info("Submitted {}", job);
    }

    //FIXME ADD xxx4 log topics
}
