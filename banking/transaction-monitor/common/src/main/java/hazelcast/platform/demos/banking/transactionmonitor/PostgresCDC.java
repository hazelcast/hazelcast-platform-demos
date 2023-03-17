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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.cdc.ChangeRecord;
import com.hazelcast.jet.cdc.postgres.PostgresCdcSources;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;

/**
 * <p>Read from Postgres change log, and save only those changes
 * to Hazelcast that didn't originate from Hazelcast.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                | Postgres Source |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                | Filter/Transform|
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 3 )------+
 *                |     Map Sink    |
 *                +-----------------+
 * </pre>
 */
public class PostgresCDC {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresCDC.class);
    private static final int POSTGRES_PORT = 5432;

    /**
     * <p>Extract data from a single table in Postgres (filter the CDC stream to ignore others)
     * and save to a single map in Hazelcast.
     * </p>
     */
    public static Pipeline buildPipeline(String address, String database, String schema,
            String user, String password, String clusterName, String mapName, String ourProvenanceProject)
                    throws Exception {

        Pipeline pipeline = Pipeline.create();

        StreamSource<ChangeRecord> postgresCdc = PostgresCDC.postgresCdc(address,
                database, schema, MyConstants.POSTGRES_TABLE_NAME, user, password, clusterName);

        pipeline
        .readFrom(postgresCdc).withoutTimestamps()
        .map(PostgresCDC.filterAndFormat(ourProvenanceProject))
        .writeTo(Sinks.map(mapName));

        return pipeline;
    }

    /**
     * <p>Connect to Postgres and stream changes from the listed schema/table
     * </p>
     *
     * @param address
     * @param database
     * @param schema
     * @param table
     * @param user
     * @param password
     * @param clusterName
     * @return
     */
    public static StreamSource<ChangeRecord> postgresCdc(String address, String database,
            String schema, String table, String user, String password, String clusterName) {

        int port = POSTGRES_PORT;
        int colon = address.indexOf(":");
        if (colon > 0) {
            port = Integer.parseInt(address.substring(colon + 1));
            address = address.substring(0, colon);
        }

        String whitelist = schema + "." + table;
        LOGGER.info("Database: '{}', whitelisting: '{}'", database, whitelist);

        return PostgresCdcSources.postgres("postgres-cdc-from-database:" + database)
                .setDatabaseAddress(address)
                .setDatabaseName(database)
                .setDatabasePassword(password)
                .setDatabasePort(port)
                .setDatabaseUser(user)
                .setReplicationSlotName(clusterName)
                .setTableWhitelist(whitelist)
                .build();
    }

    /**
     * <p>Create data object. Format should match {@link MaxAggregator}
     * and mapping defined in {@link TransactionMonitorIdempotentInitialization}.
     * </p>
     * <p>For any records on the Postgres change stream that were due to
     * a write by Hazelcast, return "{@code null}" to drop them before
     * saving to {@link IMap}.
     * </p>
     *
     * @param ourProvenanceProject "{@code transaction-monitor}"
     * @return
     */
    public static FunctionEx<ChangeRecord, Tuple2<Long, HazelcastJsonValue>>
        filterAndFormat(String ourProvenanceProject) {
        return changeRecord -> {
            try {
                Map<String, Object> keyFields = changeRecord.key().toMap();
                Map<String, Object> valueFields = changeRecord.value().toMap();

                Long key;
                if (keyFields.size() != 1) {
                    LOGGER.error("Composite key unexpected, {}", keyFields.keySet());
                    return null;
                } else {
                    // getKey() should be "now"
                    Object keyObject = keyFields.entrySet().iterator().next().getValue();
                    if (keyObject instanceof Integer) {
                        key = ((Integer) keyObject).longValue();
                    } else {
                        if (keyObject instanceof Long) {
                            key = (Long) keyObject;
                        } else {
                            LOGGER.error("Key class unexpected, {}", keyObject.getClass().getCanonicalName());
                            return null;
                        }
                    }
                }

                Object code = valueFields.get("code");
                Object provenance = valueFields.get("provenance");
                Object whence = valueFields.get("whence");
                Object volume = valueFields.get("volume");
                if (code == null || provenance == null || whence == null || volume == null) {
                    LOGGER.error("Value fields incomplete, {}", valueFields.keySet());
                    return null;
                }

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("{");
                stringBuilder.append("  \"code\" : \"" + code + "\"");
                stringBuilder.append(", \"provenance\" : \"" + provenance + "\"");
                stringBuilder.append(", \"whence\" : \"" + whence + "\"");
                stringBuilder.append(", \"volume\" : " + volume);
                stringBuilder.append("}");
                HazelcastJsonValue value = new HazelcastJsonValue(stringBuilder.toString());

                // Allow SQL scripts
                Tuple2<Long, HazelcastJsonValue> tuple2 = Tuple2.tuple2(key, value);
                if (provenance.toString().startsWith(ourProvenanceProject)) {
                    /* Anything starting "transaction-monitor" is either initial
                     * test data loaded by MapLoader or writes by Jet which
                     * we don't wish to re-read.
                     */
                    LOGGER.info("Skip re-read of own write ('{}'): {}", provenance, tuple2);
                    return null;
                } else {
                    LOGGER.info("Save: {}", tuple2);
                    return tuple2;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to process: " + changeRecord, e);
                return null;
            }
        };
    }
}
