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

package hazelcast.platform.demos.industry.iiot;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.map.IMap;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Produce periodic statistics.
 * </p>
 */
@Component
@EnableScheduling
@Slf4j
public class StatsRunnable {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private int count;

    /**
     * <p>Once a minute
     * </p>
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void run() {
        try {
            if (this.hazelcastInstance.getLifecycleService().isRunning()) {
                // Every 3rd invocation produce more detail
                String countStr = String.format("%05d", count);
                log.info("-=-=-=-=- {} '{}' {} -=-=-=-=-=-",
                        countStr, this.hazelcastInstance.getName(), countStr);
                if (count % 3 == 0) {
                    this.logDistributedObjects();
                    //XXX
                    if ("5.0".equals(System.getProperty("user.name"))) {
                        this.logJobs();
                    }
                }
                count++;
            }
        } catch (Exception e) {
            log.error("run()", e);
        }
    }

    /**
     * <p>Confirm the data sizes for {@link IMap} instances..
     * </p>
     */
    private void logDistributedObjects() {
        Collection<DistributedObject> distributedObjects = this.hazelcastInstance.getDistributedObjects();

        Set<String> mapNames = distributedObjects
                .stream()
                .filter(distributedObject -> (distributedObject instanceof IMap))
                .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .map(distributedObject -> distributedObject.getName())
                .collect(Collectors.toCollection(TreeSet::new));

        if (mapNames.isEmpty()) {
            log.info("NO MAPS");
        } else {
            mapNames
            .forEach(name -> {
                IMap<?, ?> iMap = this.hazelcastInstance.getMap(name);
                log.info("MAP '{}'.size() => {}", iMap.getName(), iMap.size());
            });
        }

        // Catch unexpected
        distributedObjects
        .stream()
        .filter(distributedObject -> !(distributedObject instanceof IMap))
        .forEach(distributedObject -> {
            String klassName = Utils.formatClientProxyClass(distributedObject.getClass());
            log.info("UNEXPECTED OBJECT, NAME '{}', CLASS '{}'",
                    distributedObject.getName(), klassName);
        });
    }

    /**
     * <p>Confirm the jobs currently running.
     * </p>
     */
    private void logJobs() {
        Map<String, Job> jobs = new TreeMap<>();
        this.hazelcastInstance
            .getJet()
            .getJobs()
            .stream()
            .forEach(job -> jobs.put(job.getName(), job));

        if (jobs.isEmpty()) {
            log.info("NO JOBS");
        } else {
            jobs
            .forEach((key, value) -> {
                log.info("JOB '{}' => {}", key, value.getStatus());
            });
        }
    }

}
