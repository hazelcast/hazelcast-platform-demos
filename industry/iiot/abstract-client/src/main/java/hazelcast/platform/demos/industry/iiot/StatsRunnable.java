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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.jet.Job;
import com.hazelcast.map.IMap;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Produce periodic statistics.
 * </p>
 */
@Component
@EnableScheduling
@Slf4j
public class StatsRunnable {
    private static final byte VERSION_FIVE = 5;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired(required = false)
    @Qualifier(MyConstants.BEAN_NAME_VERBOSE_LOGGING)
    private boolean verboseLogging;

    private int count;
    private long lastLoggingPrint;

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
                    int distributedObjectCount = this.logDistributedObjects();
                    Member member = this.hazelcastInstance.getCluster().getMembers().iterator().next();
                    // Jet added in version 5.0.0. Cluster version cannot exceed version of any member
                    if (member.getVersion().getMajor() >= VERSION_FIVE) {
                        try {
                            this.logJobs();
                        } catch (Exception e) {
                            log.error("count==" + count, e);
                        }
                    } else {
                        log.warn("Not attempting to log jobs, found member version ({}.{}.{})",
                                member.getVersion().getMajor(), member.getVersion().getMinor(), member.getVersion().getPatch());
                    }
                    // Only if eager initialization likely to have been run
                    if (distributedObjectCount >= MyConstants.IMAP_NAMES.size()) {
                        this.logLogging();
                    }
                    // Check debugging less frequently
                    if (count % (3 * 3) == 0) {
                        this.runDebuggingCallables();
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
    private int logDistributedObjects() {
        Set<String> iMapNames = new TreeSet<>();
        Set<String> executorNames = new TreeSet<>();
        Map<String, Class<?>> otherNames = new TreeMap<>();

        Collection<DistributedObject> distributedObjects = this.hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .collect(Collectors.toCollection(ArrayList::new));

        distributedObjects
                .stream()
                .forEach(distributedObject -> {
                    if (distributedObject instanceof IMap) {
                        iMapNames.add(distributedObject.getName());
                    } else {
                        if (distributedObject instanceof IExecutorService) {
                            executorNames.add(distributedObject.getName());
                        } else {
                            otherNames.put(distributedObject.getName(), distributedObject.getClass());
                        }
                    }
                });

        if (distributedObjects.isEmpty()) {
            log.info("NO DISTRIBUTED OBJECTS");
        }
        if (!executorNames.isEmpty()) {
            executorNames
            .forEach(name -> {
                log.info("EXECUTOR '{}'", name);
            });
        }
        if (!iMapNames.isEmpty()) {
            iMapNames
            .forEach(name -> {
                IMap<?, ?> iMap = this.hazelcastInstance.getMap(name);
                log.info("MAP '{}'.size() => {}", iMap.getName(), iMap.size());
            });
        }

        // Catch unexpected
        otherNames
        .entrySet()
        .forEach(entry -> {
            String klassName = Utils.formatClientProxyClass(entry.getValue());
            log.info("UNEXPECTED OBJECT, NAME '{}', CLASS '{}'",
                    entry.getKey(), klassName);
        });

        return distributedObjects.size();
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
            .forEach(job -> {
                if (job.getName() == null) {
                    if (job.isLightJob()) {
                        // Concurrent SQL doesn't have a name set.
                        log.warn("logJobs(), job.getName()==null for light job {}", job);
                    } else {
                        log.error("logJobs(), job.getName()==null for {}", job);
                    }
                } else {
                    jobs.put(job.getName(), job);
                }
            });

        if (jobs.isEmpty()) {
            log.info("NO JOBS");
        } else {
            jobs
            .forEach((key, value) -> {
                log.info("JOB '{}' => {}", key, value.getStatus());
            });
        }
    }

    /**
     * <p>Confirm what has been logged server-side.
     * </p>
     */
    private void logLogging() {
        String sql = "SELECT * FROM \"" + MyConstants.IMAP_NAME_SYS_LOGGING + "\""
                + " WHERE \"timestamp\" >= " + this.lastLoggingPrint;
        try {
            int count = 0;
            if (!this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_LOGGING).isEmpty()) {
                SqlResult sqlResult = this.hazelcastInstance.getSql().execute(sql);
                Iterator<SqlRow> sqlRowsIterator = sqlResult.iterator();
                while (sqlRowsIterator.hasNext()) {
                    SqlRow sqlRow = sqlRowsIterator.next();
                    if (this.verboseLogging) {
                        System.out.println(sqlRow);
                    }
                    count++;
                }
            }
            String date = (this.lastLoggingPrint == 0 ? "start" : new Date(this.lastLoggingPrint).toString());
            if (count == 0) {
                log.info("NO LOGS (since " + date + ")");
            } else {
                log.info(count + " LOG" + (count == 1 ? "" : "S")
                        + " (since " + date + ", verbose logging=="
                        + this.verboseLogging + ")");
            }
            this.lastLoggingPrint = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("logLogging(): " + sql, e);
        }
    }

    /**
     * <p>Invoke some callables on the cluster, see what happens.
     * </p>
     */
    private void runDebuggingCallables() {
        Debug1AllNodes debug1AllNodes = new Debug1AllNodes();
        Debug2AllNodes debug2AllNodes = new Debug2AllNodes();
        List<Callable<?>> callables = List.of(debug1AllNodes, debug2AllNodes);

        int count = 0;
        boolean ok = true;
        for (Callable<?> callable : callables) {
            if (ok) {
                ok = this.runListStringCallable(callable);
                if (ok) {
                    count++;
                }
                log.info("CALLABLE '{}' => {}", callable.getClass().getSimpleName(), ok);
            }
        }
        if (count != callables.size()) {
            log.error("{}/{} CALLABLES WORKED", count, callables.size());
        }
    }

    /**
     * <p>Run a callable on all nodes in the cluster. We expect this to return "{@code List<String>}"
     * but don't use typing to enforce this.
     * </p>
     *
     * @param callable
     * @return
     */
    private boolean runListStringCallable(Callable<?> callable) {
        IExecutorService iExecutorService = this.hazelcastInstance.getExecutorService("default");
        boolean result = true;

        //TODO This should be "Map<Member, Future<?>>"
        Map<Member, ?> futures = iExecutorService.submitToAllMembers(callable);

        for (Entry<Member, ?> entry : futures.entrySet()) {
            String node = entry.getKey().getAddress().toString();
            if (entry.getValue() instanceof Future) {
                Future<?> future = (Future<?>) entry.getValue();
                try {
                    Object o = future.get();
                    if (o instanceof Collection) {
                        Collection<?> collection = (Collection<?>) o;
                        collection.forEach(item -> log.info("Node '{}' => '{}'", node, item));
                    } else {
                        log.error("Node '{}' returned {}", node, o.getClass().getCanonicalName());
                        result = false;
                    }
                } catch (Exception e) {
                    String message = String.format("Submit '%s' to all nodes, node '%s':",
                            callable.getClass().getSimpleName(), node);
                    log.error(message, e);
                    result = false;
                }
            } else {
                log.error("Node '{}' returned {}", node, entry.getValue().getClass().getCanonicalName());
                result = false;
            }
        }

        return result;
    }

}
