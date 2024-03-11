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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>A {@link MapStore} for synching alerts to a table in Postgres.
 * </p>
 * <p>Hazelcast produces the alerts, so uses the <i>store</i> aspect
 * to ensure these are persisted to Postgres created.
 * </p>
 * <p>On a restart, previous alerts are restored into Hazelcast's
 * memory using the <i>load</i> aspect of the map store.
 * </p>
 */
@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED",
                justification = "Transient fields set after deserialize when init() called")
public class AlertingToPostgresMapStore implements MapStore<Long, HazelcastJsonValue>,
    MapLoaderLifecycleSupport, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertingToPostgresMapStore.class);
    private static final int POSTGRES_PORT = 5432;
    private static final String SQL_LOADALLKEYS = "SELECT now FROM " + MyConstants.POSTGRES_TABLE_NAME;
    private static final String SQL_LOAD = "SELECT * FROM " + MyConstants.POSTGRES_TABLE_NAME
                                        + " WHERE " + MyConstants.POSTGRES_TABLE_KEY_NAME + " = ?";
    private static final String[] TABLE_NONKEY_NAMES = new String[] { "code", "provenance", "whence", "volume"};
    private static final String SQL_STORE = "INSERT INTO " + MyConstants.POSTGRES_TABLE_NAME
            + " (" + MyConstants.POSTGRES_TABLE_KEY_NAME
            + " ," + TABLE_NONKEY_NAMES[0]
            + " ," + TABLE_NONKEY_NAMES[1]
            + " ," + TABLE_NONKEY_NAMES[2]
            + " ," + TABLE_NONKEY_NAMES[3]
            + " ) VALUES(?, ?, ?, ?, ?)";

    private transient Connection connection;
    private transient String prefix;
    private transient String ourProjectProvenance;

    /**
     * <p>Initialization, make a connection to the database.
     * </p>
     */
    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String mapName) {
        this.prefix = "'" + mapName + "'.";
        this.ourProjectProvenance = properties.getProperty(MyConstants.PROJECT_NAME, "?");
        LOGGER.trace(this.prefix + ":init() - {}", properties);

        String address = this.nullSafeGet(properties, MyConstants.POSTGRES_ADDRESS);
        if (address.indexOf(':') < 0) {
            address = address + ":" + POSTGRES_PORT;
        }
        String database = this.nullSafeGet(properties, MyConstants.POSTGRES_DATABASE);
        String schema = this.nullSafeGet(properties, MyConstants.POSTGRES_SCHEMA);

        String url = "jdbc:postgresql://" + address + "/" + database;

        Properties connectionProperties = new Properties();
        connectionProperties.put("user", this.nullSafeGet(properties, MyConstants.POSTGRES_USER));
        connectionProperties.put("password", this.nullSafeGet(properties, MyConstants.POSTGRES_PASSWORD));
        connectionProperties.put("options", "-c search_path=" + schema);

        try {
            this.connection = DriverManager.getConnection(url, connectionProperties);
            LOGGER.trace(this.prefix + ":init() - connected");
        } catch (SQLException e) {
            LOGGER.error(this.prefix + ":init(), DriverManager.getConnection(), url: {}", url);
            LOGGER.error(this.prefix + ":init(), DriverManager.getConnection(), connectionProperties: {}",
                    connectionProperties);
            LOGGER.error(this.prefix + ":init(), DriverManager.getConnection()", e);
        }
    }

    /**
     * <p>Disconnect when shutting down Hazelcast.
     * </p>
     */
    @Override
    public void destroy() {
        LOGGER.trace(this.prefix + ":destroy()");
        try {
            if (this.connection != null) {
                this.connection.close();
            }
        } catch (Exception e) {
            LOGGER.error(this.prefix + ":destroy(), this.connection.close()", e);
        }
    }

    /**
     * <p>Flag up bad properties.</p>
     */
    private String nullSafeGet(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            LOGGER.error("Bad property '{}'=='{}'", name, Objects.toString(value));
            return "null";
        } else {
            return value;
        }
    }

    /**
     * <p>Load a single row for a given primary key.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
                    justification = "'resultSet.close()' confuses FindBugs")
    public HazelcastJsonValue load(Long key) {
        LOGGER.trace(this.prefix + "load(): for key '{}': START", key);

        HazelcastJsonValue value = null;

        try (PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_LOAD);) {
            preparedStatement.setLong(1, key);
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            while (resultSet.next()) {
                StringBuffer stringBuffer = new StringBuffer("{");
                int included = 0;
                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    if (!resultSetMetaData.getColumnName(i).equals(MyConstants.POSTGRES_TABLE_KEY_NAME)) {
                        if (included++ > 0) {
                            stringBuffer.append(", ");
                        }
                        stringBuffer.append("\"").append(resultSetMetaData.getColumnName(i)).append("\" : ");
                        this.append(stringBuffer, i, resultSet, resultSetMetaData.getColumnType(i));
                    }
                }
                stringBuffer.append("}");
                value = new HazelcastJsonValue(stringBuffer.toString());
            }

            resultSet.close();
        } catch (Exception e) {
            LOGGER.error(this.prefix + ":load(), for key '" + key + "'", e);
        }

        // Debug not trace as slightly more significant to know loaded key
        LOGGER.debug(this.prefix + "load(): for key '{}': END: value '{}'", key, value);
        return value;
    }

    /**
     * <p>Append a value as JSON.
     * </p>
     *
     * @param stringBuffer
     * @param i Column in resultSet
     * @param resultSet
     * @param columnType
     * @throws SQLException
     */
    private void append(StringBuffer stringBuffer, int i, ResultSet resultSet, int columnType) throws Exception {
        switch (columnType) {
        case Types.BIGINT:
            stringBuffer.append(resultSet.getLong(i));
            break;
        case Types.NUMERIC:
            stringBuffer.append(resultSet.getFloat(i));
            break;
        case Types.VARCHAR:
            stringBuffer.append("\"").append(resultSet.getString(i)).append("\"");
            break;
        default:
            throw new RuntimeException("Unhandled type " + columnType + " for column " + i);
        }
    }

    /**
     * <p>As we don't expect to load many keys into memory, it's acceptable
     * to go for the simpler iteration than any bulk load.
     * </p>
     */
    @Override
    public Map<Long, HazelcastJsonValue> loadAll(Collection<Long> keys) {
        int expectedSize = keys.size();
        LOGGER.trace(this.prefix + "loadAll(): for {} key{}: START", expectedSize,
                (expectedSize == 1 ? "" : "s"));

        Map<Long, HazelcastJsonValue> result = new HashMap<>();
        for (Long key : keys) {
            HazelcastJsonValue json = null;
            try {
                json = this.load(key);
            } catch (Exception e) {
                LOGGER.error(this.prefix + "loadAll(): key '" + key + "'", e);
            }

            if (json != null) {
                result.put(key, json);
            }
        }

        if (result.size() != expectedSize) {
            LOGGER.error(this.prefix + "loadAll(): expected {}, got only {}", expectedSize, result.size());
        } else {
            LOGGER.trace(this.prefix + "loadAll(): for {} key{}: END", expectedSize,
                    (expectedSize == 1 ? "" : "s"));
        }

        return result;
    }

    /**
     * <p>Return the keys for the row to load, in this case all rows.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
                    justification = "'try(' confuses FindBugs")
    public Iterable<Long> loadAllKeys() {
        LOGGER.trace(this.prefix + ":loadAllKeys(): START '{}'", SQL_LOADALLKEYS);

        List<Long> keys = new ArrayList<>();
        if (this.connection != null) {
            try (PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_LOADALLKEYS);
                 ResultSet resultSet = preparedStatement.executeQuery();) {
                 while (resultSet.next()) {
                     keys.add(resultSet.getLong(1));
                 }
               } catch (Exception e) {
                   e.printStackTrace();
               }
        } else {
            LOGGER.error(this.prefix + ":loadAllKeys(): No connection");
        }

        int size = keys.size();
        // Debug, not trace, as how many returned is slightly more significant.
        LOGGER.debug(this.prefix + ":loadAllKeys(): END: {} key{}", size, (size == 1 ? "" : "s"));
        return keys;
    }

    /**
     * <p>Save a row to the database. Since CDC filters out our changes before loading,
     * anything passed to this method must be a change that originated in Hazelcast.
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "REC_CATCH_EXCEPTION"},
                    justification = "'try(' confuses FindBugs. JSON can throw exceptions")
    public void store(Long key, HazelcastJsonValue value) {
        LOGGER.trace(this.prefix + "store(): for key '{}' value '{}': START",
                key, value);

        try {
            JSONObject jsonObject = new JSONObject(value.toString());
            String provenance = jsonObject.getString("provenance");
            if (provenance == null) {
                provenance = "";
            }

            if (!provenance.startsWith(this.ourProjectProvenance)) {
                LOGGER.error(this.prefix + "store(): for key '{}' value '{}': don't save provenance '{}': END",
                        key, value, provenance);
            } else {
                try (PreparedStatement preparedStatement = this.connection.prepareStatement(SQL_STORE);) {
                    preparedStatement.setLong(1, key);
                    int offset = 2;
                    for (int i = 0; i < TABLE_NONKEY_NAMES.length; i++) {
                        if ("provenance".equals(TABLE_NONKEY_NAMES[i])) {
                            // Append module to provenance chain
                            preparedStatement.setString(i + offset, provenance + ":"
                                    + this.getClass().getSimpleName());
                        } else {
                            if ("volume".equals(TABLE_NONKEY_NAMES[i])) {
                                BigDecimal volume
                                    = new BigDecimal(jsonObject.get(TABLE_NONKEY_NAMES[i]).toString());
                                preparedStatement.setBigDecimal(i + offset, volume);
                            } else {
                                preparedStatement.setString(i + offset, jsonObject.getString(TABLE_NONKEY_NAMES[i]));
                            }
                        }
                    }

                    int count = preparedStatement.executeUpdate();

                    if (count == 1) {
                        // Debug, not trace, as storage is slightly more significant.
                        LOGGER.debug(this.prefix + "store(): for key '{}' value '{}': END",
                                key, value);
                    } else {
                        LOGGER.error(this.prefix + "store(): for key '{}' value '{}': updated {} rows: END",
                                key, value, count);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(this.prefix + ":store(), for key '" + key + "'", e);
        }
    }

    /**
     * <p>Should not be called as map store not configured in write-behind
     * mode, will get only single key {@link #store} calls.
     * </p>
     */
    @Override
    public void storeAll(Map<Long, HazelcastJsonValue> map) {
        LOGGER.error(this.prefix + "storeAll(): for {} key{}: UNEXPECTED, NO IMPLEMENTATION", map.size(),
                (map.size() == 1 ? "" : "s"));
    }

    /**
     * <p>Should not be called, don't wish to delete store of alerts from database.
     * </p>
     */
    @Override
    public void delete(Long key) {
        LOGGER.error(this.prefix + "delete(): for key '{}': UNEXPECTED, NO IMPLEMENTATION", key);
    }

    /**
     * <p>Should not be called, don't wish to delete store of alerts from database.
     * </p>
     */
    @Override
    public void deleteAll(Collection<Long> keys) {
        LOGGER.error(this.prefix + "deleteAll(): for {} key{}: UNEXPECTED, NO IMPLEMENTATION", keys.size(),
                (keys.size() == 1 ? "" : "s"));
    }

}
