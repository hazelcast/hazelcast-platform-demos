/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

/**
 * <p>Utility constants shared across the modules.
 * </p>
 */
public class MyConstants {
    public static final String APPLICATION_PROPERTIES_FILE = "application.properties";

    public static final String ALERT_LOGGER_JOB_NAME = "AlertLogger";
    public static final String ALERT_LOGGER_JOB_STATE_ON = "RUNNING";
    public static final String ALERT_LOGGER_JOB_STATE_OFF = "SUSPENDED";
    public static final String CASSANDRA_CONFIG_KEY = "my.cassandra.address";
    public static final String CASSANDRA_DATACONNECTION_CONFIG_NAME = "cassandra";
    public static final String CASSANDRA_USER = "my.cassandra.user";
    public static final String CASSANDRA_PASSWORD = "my.cassandra.password";
    public static final String CASSANDRA_TABLE = "rates";
    public static final String BOOTSTRAP_SERVERS_CONFIG_KEY = "my.bootstrap.servers";
    public static final String HOST_IP = "HOST_IP";
    public static final String KAFKA_TOPIC_MAPPING_PREFIX = "";
    public static final String KAFKA_TOPIC_NAME_ALERTS = "kf_alerts";
    public static final String KAFKA_TOPIC_NAME_TRANSACTIONS = "kf_transactions";
    public static final String MARIA_CONFIG_KEY = "my.maria.address";
    public static final String MARIA_DATACONNECTION_CONFIG_NAME = "maria";
    public static final String MARIA_USER = "my.maria.user";
    public static final String MARIA_PASSWORD = "my.maria.password";
    public static final String MARIA_TABLE = "tariff";
    public static final String MONGO_COLLECTION = "externalControl";
    public static final String MONGO_COLLECTION_FIELD1 = "jobName";
    public static final String MONGO_COLLECTION_FIELD2 = "stateRequired";
    public static final String MONGO_DATACONNECTION_CONFIG_NAME = "mongo";
    public static final String MONGO_CONFIG_KEY = "my.mongo.address";
    public static final String MONGO_USER = "my.mongo.user";
    public static final String MONGO_PASSWORD = "my.mongo.password";
    public static final String MYSQL_CONFIG_KEY = "my.mysql.address";
    public static final String MYSQL_ADDRESS = "my.mysql.address";
    public static final String MYSQL_DATACONNECTION_CONFIG_NAME = "mysql";
    public static final String MYSQL_DATACONNECTION_TABLE_NAME = "mysql_slf4j";
    public static final String MYSQL_DATACONNECTION_TABLE_COLUMN0 = "socket_address";
    public static final String MYSQL_DATACONNECTION_TABLE_COLUMN1 = "when_ts";
    public static final String MYSQL_DATACONNECTION_TABLE_COLUMN2 = "level";
    public static final String MYSQL_DATACONNECTION_TABLE_COLUMN3 = "message";
    public static final String MYSQL_DATACONNECTION_TABLE_COLUMN4 = "thread_name";
    public static final String MYSQL_DATACONNECTION_TABLE_COLUMN5 = "logger_name";
    public static final String POSTGRES_CONFIG_KEY = "my.postgres.address";
    public static final String POSTGRES_ADDRESS = "my.postgres.address";
    public static final String POSTGRES_DATABASE = "my.postgres.db";
    public static final String POSTGRES_SCHEMA = "my.postgres.schema";
    public static final String POSTGRES_USER = "my.postgres.user";
    public static final String POSTGRES_PASSWORD = "my.postgres.password";
    // Table name should match "init.sql" in Postgres module.
    public static final String POSTGRES_TABLE_NAME = "alerts_log";
    public static final String POSTGRES_TABLE_KEY_NAME = "now";
    public static final String PROJECT_MODULE = "my.project.module";
    public static final String PROJECT_NAME = "my.project.name";
    // Label used on MapStore saved, so can later identify change source
    public static final String PROJECT_PROVENANCE = PROJECT_NAME;
    public static final String PULSAR_CONFIG_KEY = "my.pulsar.address";
    public static final String PULSAR_TOPIC_NAME_TRANSACTIONS = "pulsar_transactions";
    public static final String PULSAR_OR_KAFKA_KEY = "my.pulsar.or.kafka";
    public static final String TRANSACTION_MONITOR_FLAVOR = "my.transaction-monitor.flavor";
    public static final String USE_HZ_CLOUD = "use.hz.cloud";

    public static final String IMAP_NAME_AGGREGATE_QUERY_RESULTS = "AggregateQuery" + "_results";
    public static final String IMAP_NAME_ALERTS_LOG = "alertsLog";
    public static final String IMAP_NAME_AUDIT_LOG = "audit_log";
    public static final String IMAP_NAME_BICS = "bics";
    public static final String IMAP_NAME_HEAP = "heap";
    public static final String IMAP_NAME_JOB_CONTROL = "job_control";
    public static final String IMAP_NAME_JOB_CONFIG = "job_config";
    public static final String IMAP_NAME_MONGO_ACTIONS = "mongoActions";
    public static final String IMAP_NAME_MYSQL_SLF4J = "mysql_slf4j";
    public static final String IMAP_NAME_PERSPECTIVE = "perspective";
    public static final String IMAP_NAME_PRODUCTS = "products";
    public static final String IMAP_NAME_PYTHON_SENTIMENT = "python_sentiment";
    public static final String IMAP_NAME_SYMBOLS = "symbols";
    public static final String IMAP_NAME_TRANSACTIONS = "transactions";
    public static final String IMAP_NAME_TRANSACTIONS_XML = "transactions_xml";

    public static final List<String> IMAP_NAMES_ECOMMERCE =
            List.of(IMAP_NAME_AGGREGATE_QUERY_RESULTS, IMAP_NAME_ALERTS_LOG,
                    IMAP_NAME_HEAP,
                    IMAP_NAME_JOB_CONTROL, IMAP_NAME_JOB_CONFIG, IMAP_NAME_MONGO_ACTIONS, IMAP_NAME_MYSQL_SLF4J,
                    IMAP_NAME_PRODUCTS,
                    IMAP_NAME_PERSPECTIVE, IMAP_NAME_PYTHON_SENTIMENT,
                    IMAP_NAME_TRANSACTIONS);

    public static final List<String> IMAP_NAMES_PAYMENTS =
            List.of(IMAP_NAME_AGGREGATE_QUERY_RESULTS, IMAP_NAME_ALERTS_LOG,
                    IMAP_NAME_BICS,
                    IMAP_NAME_HEAP,
                    IMAP_NAME_JOB_CONTROL, IMAP_NAME_JOB_CONFIG, IMAP_NAME_MONGO_ACTIONS, IMAP_NAME_MYSQL_SLF4J,
                    IMAP_NAME_PERSPECTIVE, IMAP_NAME_PYTHON_SENTIMENT,
                    IMAP_NAME_TRANSACTIONS, IMAP_NAME_TRANSACTIONS_XML);

    public static final List<String> IMAP_NAMES_TRADES =
            List.of(IMAP_NAME_AGGREGATE_QUERY_RESULTS, IMAP_NAME_ALERTS_LOG,
                    IMAP_NAME_HEAP,
                    IMAP_NAME_JOB_CONTROL, IMAP_NAME_JOB_CONFIG, IMAP_NAME_MONGO_ACTIONS, IMAP_NAME_MYSQL_SLF4J,
                    IMAP_NAME_PERSPECTIVE, IMAP_NAME_PYTHON_SENTIMENT,
                    IMAP_NAME_SYMBOLS, IMAP_NAME_TRANSACTIONS);

    // Maps that are replicated over WAN in enterprise only. Avoid clashing with
    // TransactionMonitorIdempotentInitialization.java configures some maps with map stores and journals
    public static final List<String> WAN_IMAP_NAMES_ECOMMERCE =
            List.of(IMAP_NAME_AUDIT_LOG, IMAP_NAME_JOB_CONFIG, IMAP_NAME_PRODUCTS);
    public static final List<String> WAN_IMAP_NAMES_PAYMENTS =
            List.of(IMAP_NAME_AUDIT_LOG, IMAP_NAME_JOB_CONFIG, IMAP_NAME_BICS);
    public static final List<String> WAN_IMAP_NAMES_TRADE =
            List.of(IMAP_NAME_AUDIT_LOG, IMAP_NAME_JOB_CONFIG, IMAP_NAME_SYMBOLS);

    public static final String PN_UPDATER = "updater-" + PerspectiveTrade.class.getSimpleName();

    public static final String VECTOR_COLLECTION_TRANSACTIONS = "transactions";
    public static final String VECTOR_DOCUMENT_MOMENTS = "moments";
    public static final int MOMENTS_IN_HOUR = 40;

    public static final String WEBSOCKET_PATH_TRANSACTIONS = "/transactions";

    // For demonstration of queries
    public static final String SQL_JOB_NAME_IMAP_TO_KAFKA = "imap2Kafka";
    public static final String SQL_JOB_NAME_KAFKA_TO_IMAP = "kafka2IMap";
    public static final String SQL_JOB_NAME_MONGO_TO_IMAP = "mongo2IMap";
    public static final int SQL_RESULT_THRESHOLD = 10;
    // For SQL VIEW naming
    public static final String VIEW_SUFFIX = "_VIEW";

    // Tiered and Persistent Store, only used by Enterprise grids
    public static final String PERSISTENT_STORE_SUFFIX = "ps";
    public static final List<String> PERSISTENT_STORE_IMAP_NAMES_ECOMMERCE =
            List.of(IMAP_NAME_PRODUCTS);
    public static final List<String> PERSISTENT_STORE_IMAP_NAMES_PAYMENTS =
            List.of(IMAP_NAME_BICS);
    public static final List<String> PERSISTENT_STORE_IMAP_NAMES_TRADE =
            List.of(IMAP_NAME_SYMBOLS);
    public static final String STORE_BASE_DIR_PREFIX = "data/transaction-monitor";
    public static final String TIERED_STORE_SUFFIX = "ts";
    public static final String TIERED_STORE_DEVICE_NAME = "transaction-monitor-local";
    public static final List<String> TIERED_STORE_IMAP_NAMES_ECOMMERCE =
            List.of(IMAP_NAME_TRANSACTIONS);
    public static final List<String> TIERED_STORE_IMAP_NAMES_PAYMENTS =
            List.of(IMAP_NAME_TRANSACTIONS, IMAP_NAME_TRANSACTIONS_XML);
    public static final List<String> TIERED_STORE_IMAP_NAMES_TRADE =
            List.of(IMAP_NAME_TRANSACTIONS);
    public static final long TIERED_STORE_DISK_CAPACITY_GB = 10;
    public static final long TIERED_STORE_MEMORY_CAPACITY_MB = 256;

    // For exponential loggers
    public static final int MAX_LOGGING_INTERVAL = 3_000;

    // Perspective fields, Generic Record
    public static final String PERSPECTIVE_FIELD_AVERAGE = "average";
    public static final String PERSPECTIVE_FIELD_BIC = "bic";
    public static final String PERSPECTIVE_FIELD_CODE = "code";
    public static final String PERSPECTIVE_FIELD_COUNT = "count";
    public static final String PERSPECTIVE_FIELD_LATEST = "latest";
    public static final String PERSPECTIVE_FIELD_RANDOM = "random";
    public static final String PERSPECTIVE_FIELD_SECONDS = "seconds";
    public static final String PERSPECTIVE_FIELD_SUM = "sum";
    public static final String PERSPECTIVE_FIELD_SYMBOL = "symbol";
    public static final String PERSPECTIVE_JSON_KEY = "key";
    public static final String PERSPECTIVE_JSON_DERIVED = "derived";
    // Web Sockets, for Finos module
    public static final String WEBSOCKET_ENDPOINT = "hazelcast";
    public static final String WEBSOCKET_FEED_PREFIX = "feed";
    public static final String WEBSOCKET_DATA_SUFFIX = "data";

    public static final int CP_GROUP_SIZE = 3;
    public static final int CP_MEMBER_SIZE = CP_GROUP_SIZE;
    public static final String CP_GROUP_A = "odd";
    public static final String CP_GROUP_B = "even";

    // Objects in namespaces
    public static final String EXECUTOR_NAMESPACE_1 = "executor_ns1";
    public static final String EXECUTOR_NAMESPACE_2 = "executor_ns2";
    public static final String EXECUTOR_NAMESPACE_3 = "executor_ns3";
    public static final String MAP_NAMESPACE_2 = "map_ns2";
    public static final String QUEUE_NAMESPACE_3 = "queue_ns3";
    // Namespace names
    public static final String USER_CODE_NAMESPACE_1 = "ns1";
    public static final String USER_CODE_NAMESPACE_2 = "ns2";
    public static final String USER_CODE_NAMESPACE_3 = "ns3";
    // These are copied into the Dockerfile for WebApp.
    public static final String USER_CODE_JAR_FOR_NAMESPACE_1 = "namespace1.jar";
    public static final String USER_CODE_JAR_FOR_NAMESPACE_2 = "namespace2.jar";
    public static final String USER_CODE_JAR_FOR_NAMESPACE_3 = "namespace3.jar";

}
