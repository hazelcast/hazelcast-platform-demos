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

package com.hazelcast.platform.demos.telco.churn;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.cdc.ChangeRecord;
import com.hazelcast.jet.cdc.Operation;
import com.hazelcast.jet.cdc.mysql.MySqlCdcSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.platform.demos.telco.churn.domain.TariffMetadata;

/**
 * <p>A job to simply upload and reformat change data records
 * from MySql into a Hazelcast {@link IMap}.
 * </p>
 * <p><b>NOTE</b> Hazelcast for MySql (but not Cassandra) does
 * not save back to MySql. So we don't get any CDC records that
 * originate from our own changes to stop.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                |  MySql Source   |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                |    Reformat     |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 3 )------+
 *                |    IMap Sink    |
 *                +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * MySql source
 * </p>
 * <p>Use a CDC connector to track changes to the "{@code tariff}"
 * table in MySql.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformat
 * </p>
 * <p>Turn the CDC record into a "{@code Map<String, HazelcastJsonValue}"
 * record.
 * </p>
 * </li>
 * <li>
 * <p>
 * Map sink
 * </p>
 * <p>Save everything read into a map.
 * </p>
 * </li>
 * </ol>
 */
public class MySqlDebeziumOneWayCDC extends MyJobWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlDebeziumOneWayCDC.class);
    private static final int MYSQL_PORT = 3306;

    private String mySqlHost;
    private String mySqlUsername;
    private String mySqlPassword;

    MySqlDebeziumOneWayCDC(long timestamp, String username, String password) {
        super(timestamp);
        this.mySqlUsername = username;
        this.mySqlPassword = password;

        // Configure expected MySql address for Docker or Kubernetes
        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            this.mySqlHost =
                 System.getProperty("my.project") + "-mysql.default.svc.cluster.local";

            LOGGER.info("Kubernetes configuration: mysql host: '{}'", this.mySqlHost);
        } else {
            this.mySqlHost = "mysql";
            LOGGER.info("Non-Kubernetes configuration: mysql host: '{}'", this.mySqlHost);
        }
    }

    /**
     * <p>Create the pipeline.
     * </p>
     */
    public Pipeline getPipeline() {
        Pipeline pipeline = Pipeline.create();

        StreamSource<ChangeRecord> mySqlCdcStreamSource = buildMySqlCdcStreamSource();

        pipeline
        .readFrom(mySqlCdcStreamSource).withoutTimestamps()
        .map(changeRecord ->
            cdcToEntry(changeRecord.operation(), changeRecord.timestamp(), changeRecord.value().toJson()))
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_TARIFF));

        return pipeline;
    }

    /**
     * <p>Configure a MySql stream source for one specific table.
     * Wildcard the server name.</p>
     *
     * @return
     */
    private StreamSource<ChangeRecord> buildMySqlCdcStreamSource() {
        String dbServerName = "_";
        String dbSchema = MyConstants.MYSQL_SCHEMA_NAME;
        String dbSchemaTable = dbSchema + "." + MyConstants.MYSQL_TABLE_NAME;

        return MySqlCdcSources.mysql(this.getClass().getSimpleName() + "StreamSource")
                .setDatabaseAddress(this.mySqlHost)
                .setDatabasePort(MYSQL_PORT)
                .setDatabaseUser(this.mySqlUsername)
                .setDatabasePassword(this.mySqlPassword)
                .setClusterName(dbServerName)
                .setDatabaseWhitelist(dbSchema)
                .setTableWhitelist(dbSchemaTable)
                .build();
    }

    /**
     * <p>Reformat a CDC value into the map entry. Don't need to validate
     * as this is done already in {@link TariffMapLoader}, and we're
     * assuming not columns added while running. Also, it may have
     * extra unexpected metadata columns.
     * </p>
     * <p>Optional: Add typing information to {@link TariffMetadata} for
     * cleaner parsing, deduce rather than code types, and non-direct
     * field name mappings ("{@code rate}" to "{@code ratePerMinute}").
     * </p>
     *
     * @param operation
     * @param timestamp
     * @param value
     * @return
     */
    protected static Map.Entry<String, HazelcastJsonValue> cdcToEntry(Operation operation,
            long timestamp, String valueAsJson) {
        LOGGER.debug("cdcToEntry({}, {}, '{}')", operation, new Date(timestamp), valueAsJson);

        JSONObject json = new JSONObject(valueAsJson);
        String key = null;

        StringBuilder stringBuilder = new StringBuilder("{ ");
        for (int i = 0 ; i < TariffMetadata.getFieldNames().size(); i++) {
            if (i != 0) {
                stringBuilder.append(", ");
            }

            String name = TariffMetadata.getFieldNames().get(i);
            stringBuilder.append("\"" + name + "\" : ");

            switch (name) {
                case TariffMetadata.ID:
                case TariffMetadata.NAME:
                    stringBuilder.append("\"" + json.get(name).toString() + "\"");
                    break;
                case TariffMetadata.INTERNATIONAL:
                    // MySql uses 0 & 1
                    int j = json.getInt(name);
                    if (j == 0) {
                        stringBuilder.append("false");
                    } else {
                        stringBuilder.append("true");
                    }
                    break;
                case TariffMetadata.YEAR:
                    stringBuilder.append(json.getInt(name));
                    break;
                case TariffMetadata.RATE_PER_MINUTE:
                    // Field in JSON is not same as column name in MySql
                    stringBuilder.append(json.getDouble("rate"));
                    break;
                default:
                    LOGGER.error("Field '{}' unexpected in '{}'", name, valueAsJson);
                    stringBuilder.append("\"\"");
            }

            if (TariffMetadata.ID.equals(name)) {
                key = json.get(name).toString();
            }
        }
        stringBuilder.append(" }");

        if (key == null) {
            LOGGER.error("Didn't find key in '{}'", valueAsJson);
            return null;
        } else {
            return new SimpleImmutableEntry<String, HazelcastJsonValue>(key,
                    new HazelcastJsonValue(stringBuilder.toString()));
        }
    }
}
