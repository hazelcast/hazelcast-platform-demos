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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;

import ch.qos.logback.classic.Level;
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

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                this.hazelcastInstance.getName());

            if ("".equals(System.getProperty("HOST_IP", ""))) {
                log.warn("Cloud mode");
                this.initialize(Level.INFO);
                this.listAndDeleteDistributedObject();
            } else {
                log.warn("Local mode");
                this.initialize(Level.TRACE);
            }

            TimeUnit.SECONDS.sleep(2L);
            this.showMappings();
            this.queryMap(MyConstants.IMAP_NAME_SERVICE_HISTORY);
            this.queryMap(MyConstants.IMAP_NAME_SYS_CONFIG);
            this.queryMap(MyConstants.IMAP_NAME_SYS_LOGGING);
            TimeUnit.HOURS.sleep(1L);

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
            { "a", "bb"},
            { "c", "d"},
        };

        log.info("Logging level '{}'", level);
        InitializerAllNodes initializerAllNodes = new InitializerAllNodes(level);
        boolean ok = Utils.runTuple4Callable(initializerAllNodes, this.hazelcastInstance, false);
        if (ok) {
            InitializerAnyNode initializerAnyNode = new InitializerAnyNode(config);
            ok = Utils.runTuple4Callable(initializerAnyNode, this.hazelcastInstance, true);
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
            SqlResult sqlResult = this.hazelcastInstance.getSql().execute(sql);
            Iterator<SqlRow> sqlRowsIterator = sqlResult.iterator();
            while (sqlRowsIterator.hasNext()) {
                SqlRow sqlRow = sqlRowsIterator.next();
                System.out.println(sqlRow);
            }
        } catch (Exception e) {
            log.error("runQuery: " + sql, e);
        }
    }

}
