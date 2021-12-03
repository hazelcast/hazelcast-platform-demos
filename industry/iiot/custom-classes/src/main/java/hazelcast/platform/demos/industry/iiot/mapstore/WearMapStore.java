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

package hazelcast.platform.demos.industry.iiot.mapstore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.MapStore;
import com.zaxxer.hikari.HikariDataSource;
import com.hazelcast.map.MapLoaderLifecycleSupport;

import hazelcast.platform.demos.industry.iiot.MyConstants;
import hazelcast.platform.demos.industry.iiot.Utils;

/**
 * <p>Write values to Maria. This version does not read from Maria.
 * </p>
 * <p>Based on <a href="https://github.com/hazelcast/hazelcast-cloud-code-samples
/tree/master/mapstore/mapstore-sample-hazelcast4-jdbc">this example</a>.
 * </p>
 */
public class WearMapStore implements MapStore<String, HazelcastJsonValue>,
    MapLoaderLifecycleSupport, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(WearMapStore.class);

    private transient HikariDataSource hikariDataSource;
    private String database;
    private String host;
    private String mapName;
    private String password;
    private String username;

    public WearMapStore(Map<String, String> mongoProperties, String mapName) {
        this.database = mongoProperties.getOrDefault(MyConstants.MARIA_DATABASE, "");
        this.host = mongoProperties.getOrDefault(MyConstants.MARIA_HOST, "");
        this.password = mongoProperties.getOrDefault(MyConstants.MARIA_PASSWORD, "");
        this.username = mongoProperties.getOrDefault(MyConstants.MARIA_USERNAME, "");
        String[][] config = new String[][] {
            { MyConstants.MARIA_DATABASE, this.database },
            { MyConstants.MARIA_PASSWORD, this.password },
            { MyConstants.MARIA_USERNAME, this.username },
            { MyConstants.MARIA_HOST, this.host},
        };
        for (String[] pair : config) {
            if (pair[1].length() == 0) {
                String message = String.format("Bad Maria property: '%s' == '%s'", pair[0], pair[1]);
                throw new RuntimeException(message);
            }
        }
        this.mapName = mapName;
    }

    /**
     * <p>Attempt to connect, using the provided properties.
     * </p>
     */
    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String mapName) {
        LOGGER.trace(this.mapName + ":init(), START");
        HikariDataSource tmpHikariDataSource = null;

        // Exceptions are more likely with the connection
        try {
            // Default port 3306, credentials for user in admin database
            String url = "jdbc:mariadb://" + this.host + ":3306/" + this.database;
            tmpHikariDataSource = new HikariDataSource();
            tmpHikariDataSource.setAutoCommit(false);
            tmpHikariDataSource.setDriverClassName("org.mariadb.jdbc.Driver");
            tmpHikariDataSource.setJdbcUrl(url);
            tmpHikariDataSource.setPassword(this.password);
            tmpHikariDataSource.setUsername(this.username);
        } catch (Exception e) {
            Utils.addExceptionToLogger(LOGGER, "init()", e);
        }

        // Only use if fully initialised and connected
        if (tmpHikariDataSource != null) {
            this.hikariDataSource = tmpHikariDataSource;
        }
        LOGGER.trace(this.mapName + ":init(), END OK==" + Boolean.valueOf(this.hikariDataSource != null));
    }

    /**
     * <p>Disconnect, in the unlikely case this class is destroyed.
     * </p>
     */
    @Override
    public void destroy() {
        if (this.hikariDataSource != null) {
            this.hikariDataSource.close();
        }
        LOGGER.trace(this.mapName + ":destroy()");
    }

    /**
     * <p>Placeholder implementation, as write-only this read method should not be
     * called.
     * </p>
     */
    @Override
    public HazelcastJsonValue load(String key) {
        LOGGER.trace(this.mapName + ":load(" + key + ") START");

        // MapStore read should not be invoked
        LOGGER.error(this.mapName + ":load(" + key + ") UNEXPECTED, NO IMPLEMENTATION");

        //HazelcastJsonValue result = null;

        //LOGGER.trace("load(" + key + ") END, '" + Utils.escapeJson(result) + "'");
        LOGGER.trace("load(" + key + ") END, 'null'");
        return null;
    }

    /**
     * <p>Retrieve a partition's worth of data records.
     * </p>
     * <p>Placeholder implementation, as write-only this read method should not be
     * called.
     * </p>
     */
    @Override
    public Map<String, HazelcastJsonValue> loadAll(Collection<String> keys) {
        LOGGER.trace(this.mapName + ":loadAll(" + new ArrayList<>(keys) + ")");

        // MapStore read should not be invoked
        LOGGER.error(this.mapName + ":loadAll(" + new ArrayList<>(keys) + ") UNEXPECTED, NO IMPLEMENTATION");

        Map<String, HazelcastJsonValue> result = Collections.emptyMap();

        if (result.size() != keys.size()) {
            LOGGER.warn(this.mapName + ":loadAll(" + new ArrayList<>(keys) + ") got only " + result.size());
        }
        return result;
    }

    /**
     * <p>Retrieve this primary keys from Maria that are to be loaded. This would usually
     * be all of them, or the most vital subset. However, as Maria is used write-only
     * and we don't want reads, we return an empty list, no need to check Maria.
     * </p>
     */
    @Override
    public Iterable<String> loadAllKeys() {
        LOGGER.debug(this.mapName + ":loadAllKeys(), START");
        List<String> ids = Collections.emptyList();
        LOGGER.debug(this.mapName + ":loadAllKeys(), END, " + ids.size());
        return ids;
    }

    //FIXME
    @Override
    public void store(String key, HazelcastJsonValue value) {
        // TODO Auto-generated method stub
    }

    //FIXME
    @Override
    public void storeAll(Map<String, HazelcastJsonValue> map) {
        // TODO Auto-generated method stub
    }

    //FIXME
    @Override
    public void delete(String key) {
        // TODO Auto-generated method stub
    }

    //FIXME
    @Override
    public void deleteAll(Collection<String> keys) {
        // TODO Auto-generated method stub
    }

}
