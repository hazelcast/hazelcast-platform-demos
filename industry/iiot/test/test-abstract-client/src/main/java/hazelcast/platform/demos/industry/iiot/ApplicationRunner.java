/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Run for a while.
 * </p>
 * <p>Other scheduled tasks such as {@link StatsRunnable}
 * will produce intermittent output.
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationRunner {
    private static final int FOUR = 4;
    private static final int FIFTEEN = 15;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyProperties myProperties;
    @Value("${spring.application.name}")
    private String springApplicationName;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                this.hazelcastInstance.getName());

            boolean ok;
            if ("".equals(System.getProperty("HOST_IP", ""))) {
                log.warn("Cloud mode");
                ok = this.initialize(Level.INFO);
                if (ok) {
                    this.listAndDeleteDistributedObject();
                }
            } else {
                log.warn("Local mode");
                ok = this.initialize(Level.TRACE);
            }

            if (ok) {
                TimeUnit.SECONDS.sleep(2L);
                System.out.println("");
                this.showMappings();
                System.out.println("");
                this.queryMap(MyConstants.IMAP_NAME_SERVICE_HISTORY);
                System.out.println("");
                this.queryMap(MyConstants.IMAP_NAME_SYS_CONFIG);
                System.out.println("");

                TimeUnit.MINUTES.sleep(2L);

                // Do something every few minutes for an hour
                int count = FIFTEEN;
                while (count > 0 && ok && this.hazelcastInstance.getLifecycleService().isRunning()) {
                    String countStr = String.format("%05d", count);
                    log.info("-=-=-=-=- {} '{}' {} -=-=-=-=-=-",
                            countStr, this.hazelcastInstance.getName(), countStr);

                    String caller = this.springApplicationName + "-" + myProperties.getBuildTimestamp() + "-"
                            + myProperties.getBuildUserName();
                    LoggingNoOpAnyNode loggingNoOpAnyNode = new LoggingNoOpAnyNode(caller);
                    ok = Utils.runTuple4Callable(log, loggingNoOpAnyNode, this.hazelcastInstance, true);

                    if (ok) {
                        TimeUnit.MINUTES.sleep(FOUR);
                    }
                    count--;
                }
            }

            log.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-",
                    this.hazelcastInstance.getName());
        };
    }

    /**
     * <p>List and delete the objects in the cluster.
     * Presume these just to be {@link IMap}.
     * </p>
     */
    @SuppressWarnings("rawtypes")
    private void listAndDeleteDistributedObject() {
        Collection<DistributedObject> distributedObjects = this.hazelcastInstance.getDistributedObjects()
                .stream()
                .filter(distributedObject -> !distributedObject.getName().startsWith("__"))
                .collect(Collectors.toCollection(ArrayList::new));

        log.info("-------------------------------");
        log.info("Delete:");

        int count = 0;
        Set<String> iMapNames = new TreeSet<>();
        for (DistributedObject distributedObject : distributedObjects) {
            if (distributedObject instanceof IMap) {
                iMapNames.add(distributedObject.getName());
            } else {
                // Unexpected type
                String klassName = Utils.formatClientProxyClass(distributedObject.getClass());
                if (klassName.equals("ExecutorService")) {
                    log.info(String.format("  : %15s: %s", "'" + distributedObject.getName() + "'", klassName));
                } else {
                    log.error(String.format("  : %15s: %s unexpected", "'" + distributedObject.getName() + "'", klassName));
                }
                distributedObject.destroy();
                count++;
            }
        }

        for (String iMapName : iMapNames) {
            IMap iMap = this.hazelcastInstance.getMap(iMapName);
            log.info(String.format("  : %15s: IMap, size()==%d", "'" + iMap.getName() + "'", iMap.size()));
            iMap.destroy();
            count++;
        }

        if (count != distributedObjects.size()) {
            throw new RuntimeException(String.format("Deleted %d instead of %d", count, distributedObjects.size()));
        }
        if (count == 0) {
            log.warn("** Nothing to delete **");
        } else {
            log.info("** {} deleted **", count);
        }
        log.info("-------------------------------");
    }

    /**
     * <p>Initialize the cluster, creating maps, loading any necessary data.
     * </p>
     */
    private boolean initialize(Level level) {
        String[][] config = new String[][] {
            { MyConstants.MARIA_DATABASE, this.myProperties.getMariaDatabase() },
            { MyConstants.MARIA_PASSWORD, this.myProperties.getMariaPassword() },
            { MyConstants.MARIA_USERNAME, this.myProperties.getMariaUsername() },
            //FIXME temporary ?
            { MyConstants.MARIA_HOST, System.getProperty("HOST_IP", "") },
            { MyConstants.MONGO_COLLECTION1, this.myProperties.getMongoCollection1() },
            { MyConstants.MONGO_DATABASE, this.myProperties.getMongoDatabase() },
            { MyConstants.MONGO_PASSWORD, this.myProperties.getMongoPassword() },
            { MyConstants.MONGO_USERNAME, this.myProperties.getMongoUsername() },
            //FIXME temporary ?
            { MyConstants.MONGO_HOST, System.getProperty("HOST_IP", "") },
        };

        log.info("Logging level for server-side '{}'", level);
        InitializerAllNodes initializerAllNodes = new InitializerAllNodes(level);
        boolean ok = Utils.runTuple4Callable(log, initializerAllNodes, this.hazelcastInstance, false);
        if (ok) {
            InitializerAnyNode initializerAnyNode = new InitializerAnyNode(config);
            ok = Utils.runTuple4Callable(log, initializerAnyNode, this.hazelcastInstance, true);
        }
        if (!ok) {
            log.error("initialize(): FAILED");
        }
        return ok;
    }

    private void showMappings() {
        String sql = "SHOW MAPPINGS";
        this.runQuery(sql);
    }

    private void queryMap(String mapName) {
        String sql = "SELECT * FROM \"" + mapName + "\"";
        this.runQuery(sql);
    }

    private void runQuery(String sql) {
        try {
            System.out.println(sql);
            SqlResult sqlResult = this.hazelcastInstance.getSql().execute(sql);
            Iterator<SqlRow> sqlRowsIterator = sqlResult.iterator();
            int count = 0;
            while (sqlRowsIterator.hasNext()) {
                SqlRow sqlRow = sqlRowsIterator.next();
                System.out.println("==> " + sqlRow);
                count++;
            }
            System.out.println("[" + count + " row" + (count == 1 ? "" : "s") + "]");
        } catch (Exception e) {
            log.error("runQuery: " + sql, e);
        }
    }

}
