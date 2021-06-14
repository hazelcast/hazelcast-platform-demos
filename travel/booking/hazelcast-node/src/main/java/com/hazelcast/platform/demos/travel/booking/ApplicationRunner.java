/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import com.hazelcast.map.IMap;

/**
 * <p>Do not directly add much to the node from the same container.
 * This is done by the "{code hazelcast-initializer}" module.
 * </p>
 */
@Configuration
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final int FIVE = 5;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * <p>Peridically log to the console, so there is something to
     * confirm member is ok.
     * </p>
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            LOGGER.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                this.hazelcastInstance.getName());

            int count = 0;
            while (this.hazelcastInstance.getLifecycleService().isRunning()) {
                TimeUnit.MINUTES.sleep(1);
                count++;
                String countStr = String.format("%05d", count);
                LOGGER.info("-=-=-=-=- {} '{}' {} -=-=-=-=-=-",
                        countStr, this.hazelcastInstance.getName(), countStr);
                if (count % FIVE == 0) {
                    this.logSizes();
                    this.logJobs();
                }
            }
        };
    }

    /**
     * <p>"{@code size()}" is a relatively expensive operation, but we
     * only run this every few minutes.
     * </p>
     */
    private void logSizes() {
        Set<String> mapNames = this.hazelcastInstance
                .getDistributedObjects()
                .stream()
                .filter(distributedObject -> (distributedObject instanceof IMap))
                .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .map(distributedObject -> distributedObject.getName())
                .collect(Collectors.toCollection(TreeSet::new));

        mapNames
        .forEach(name -> {
            IMap<?, ?> iMap = this.hazelcastInstance.getMap(name);
            LOGGER.info("MAP '{}'.size() => {}", iMap.getName(), iMap.size());
        });
    }

    /**
     * <p>Jobs in name order.
     * </p>
     */
    private void logJobs() {
        Map<String, Job> jobs = new TreeMap<>();
        this.hazelcastInstance
            .getJet()
            .getJobs()
            .stream()
            .forEach(job -> jobs.put(job.getName(), job));

        jobs
        .forEach((key, value) -> {
            LOGGER.info("JOB '{}' => {}", key, value.getStatus());
        });
    }
}
