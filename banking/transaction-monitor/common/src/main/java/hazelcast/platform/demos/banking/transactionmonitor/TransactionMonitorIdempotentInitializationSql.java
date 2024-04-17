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

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.platform.demos.utils.UtilsJobs;

/**
 * <p>SQL jobs
 * </p>
 * <p>Invoked by the overlarge {@link TransactionMonitorIdempotentInitialization}
 * </p>
 */
public class TransactionMonitorIdempotentInitializationSql {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMonitorIdempotentInitializationSql.class);
    private static final Logger LOGGER_TO_IMAP =
            IMapLoggerFactory.getLogger(TransactionMonitorIdempotentInitializationSql.class);

    /**
     * <p>Use SQL to stream a Kafka topic into an IMap
     * </p>
     * <p>Use SQL to stream an IMap into an a Kafka topic
     * </p>
     *
     * @param hazelcastInstance
     * @param bootstrapServers
     * @param transactionMonitorFlavor
     */
    public static boolean launchKafkaSqlJobs(HazelcastInstance hazelcastInstance, String bootstrapServers,
            TransactionMonitorFlavor transactionMonitorFlavor) {
        String topic = MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_ALERTS;

        String sqlJobKafkaToMap =
                "CREATE JOB IF NOT EXISTS \"" + MyConstants.SQL_JOB_NAME_KAFKA_TO_IMAP + "\""
                + " AS "
                + " SINK INTO \"" + MyConstants.IMAP_NAME_AUDIT_LOG + "\""
                + " SELECT * FROM \"" + topic + "\"";

        String concatenation;
        String xxx = "5.x_";
        LOGGER.error("Reminder to remove job prefix {}", xxx);
        // Same for all currently
        switch (transactionMonitorFlavor) {
        //case ECOMMERCE:
        //    concatenation = "code";
        //    break;
        //case PAYMENTS:
        //    concatenation = "code";
        //    break;
        //case TRADE:
        default:
            concatenation = "code";
            break;
        }
        String sqlJobMapToKafka =
                "CREATE JOB IF NOT EXISTS \"" + xxx + MyConstants.SQL_JOB_NAME_IMAP_TO_KAFKA + "\""
                + " AS "
                + " SINK INTO \"" + MyConstants.KAFKA_TOPIC_NAME_ALERTS + "\""
                + " SELECT __key, " + concatenation + " || ',' || provenance || ',' || whence || ',' || volume"
                + " FROM \"" + MyConstants.IMAP_NAME_ALERTS_LOG + "\"";

        //TODO 5.4 Style, to be removed once "sqlJobMapToKafka" runs as streaming in 5.x
        //TODO https://docs.hazelcast.com/hazelcast/5.4-snapshot/sql/querying-maps-sql#streaming-map-changes
        //TODO See https://github.com/hazelcast/hazelcast-platform-demos/issues/131
        try {
            Pipeline pipelineAlertingToKafka = AlertingToKafka.buildPipeline(bootstrapServers);

            JobConfig jobConfigAlertingToKafka = new JobConfig();
            jobConfigAlertingToKafka.setName(AlertingToKafka.class.getSimpleName());
            jobConfigAlertingToKafka.addClass(HazelcastJsonValueSerializer.class);

            Job job = UtilsJobs.myNewJobIfAbsent(LOGGER, hazelcastInstance, pipelineAlertingToKafka, jobConfigAlertingToKafka);
            if (job != null) {
                LOGGER_TO_IMAP.info(Objects.toString(job));
            }
        } catch (Exception e) {
            LOGGER.error("launchAlertsSqlToKafka:", e);
            return false;
        }

        //TODO Submit "sqlJobMapToKafka", will complete until ready for streaming as reminder
        for (String sql : List.of(sqlJobKafkaToMap, sqlJobMapToKafka)) {
            try {
                hazelcastInstance.getSql().execute(sql);
                LOGGER.info("SQL running: '{}'", sql);
            } catch (Exception e) {
                LOGGER.error("launchKafkaSqlJobs:" + sql, e);
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Use SQL to stream a Mongo collection into an IMap
     * </p>
     *
     * @param hazelcastInstance
     */
    public static boolean launchNonKafkaSqlJobs(HazelcastInstance hazelcastInstance) {

        String sqlJobMongoToMap =
                "CREATE JOB IF NOT EXISTS \"" + MyConstants.SQL_JOB_NAME_MONGO_TO_IMAP + "\""
                + " AS "
                + " SINK INTO \"" + MyConstants.IMAP_NAME_MONGO_ACTIONS + "\""
                + " SELECT \"fullDocument._id\" AS _id,"
                +          "\"fullDocument.jobName\" AS jobName,"
                +          "\"fullDocument.stateRequired\" AS stateRequired FROM \"" + MyConstants.MONGO_COLLECTION + "\"";

        for (String sql : List.of(sqlJobMongoToMap)) {
            try {
                hazelcastInstance.getSql().execute(sql);
                LOGGER.info("SQL running: '{}'", sql);
            } catch (Exception e) {
                LOGGER.error("launchNonKafkaSqlJobs:" + sql, e);
                return false;
            }
        }
        return true;
    }

}
