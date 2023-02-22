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

import java.util.List;

/**
 * <p>Utility constants shared across the modules.
 * </p>
 */
public class MyConstants {
    public static final String APPLICATION_PROPERTIES_FILE = "application.properties";

    public static final String KAFKA_TOPIC_MAPPING_PREFIX = "";
    public static final String KAFKA_TOPIC_NAME_ALERTS = "kf_alerts";
    public static final String KAFKA_TOPIC_NAME_TRANSACTIONS = "kf_transactions";
    public static final String MYSQL_ADDRESS = "my.mysql.address";
    public static final String MYSQL_DATASTORE_CONFIG_NAME = "mysql";
    public static final String MYSQL_DATASTORE_TABLE_NAME = "mysql_slf4j";
    public static final String MYSQL_DATASTORE_TABLE_COLUMN0 = "socket_address";
    public static final String MYSQL_DATASTORE_TABLE_COLUMN1 = "when_ts";
    public static final String MYSQL_DATASTORE_TABLE_COLUMN2 = "level";
    public static final String MYSQL_DATASTORE_TABLE_COLUMN3 = "message";
    public static final String MYSQL_DATASTORE_TABLE_COLUMN4 = "thread_name";
    public static final String MYSQL_DATASTORE_TABLE_COLUMN5 = "logger_name";
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
    public static final String PULSAR_CONFIG_KEY = "my.pulsar.list";
    public static final String PULSAR_TOPIC_NAME_TRANSACTIONS = "pulsar_transactions";
    public static final String PULSAR_OR_KAFKA_KEY = "my.pulsar.or.kafka";
    public static final String TRANSACTION_MONITOR_FLAVOR = "my.transaction-monitor.flavor";
    public static final String USE_VIRIDIAN = "use.viridian";

    public static final String IMAP_NAME_AGGREGATE_QUERY_RESULTS = "AggregateQuery" + "_results";
    public static final String IMAP_NAME_ALERTS_LOG = "alertsLog";
    public static final String IMAP_NAME_AUDIT_LOG = "audit_log";
    public static final String IMAP_NAME_BICS = "bics";
    public static final String IMAP_NAME_JOB_CONTROL = "job_control";
    public static final String IMAP_NAME_JOB_CONFIG = "job_config";
    public static final String IMAP_NAME_MYSQL_SLF4J = "mysql_slf4j";
    public static final String IMAP_NAME_PERSPECTIVE = "perspective";
    public static final String IMAP_NAME_PRODUCTS = "products";
    public static final String IMAP_NAME_PYTHON_SENTIMENT = "python_sentiment";
    public static final String IMAP_NAME_SYMBOLS = "symbols";
    public static final String IMAP_NAME_TRANSACTIONS = "transactions";
    public static final String IMAP_NAME_TRANSACTIONS_XML = "transactions_xml";

    public static final List<String> IMAP_NAMES_ECOMMERCE =
            List.of(IMAP_NAME_AGGREGATE_QUERY_RESULTS, IMAP_NAME_ALERTS_LOG,
                    IMAP_NAME_JOB_CONTROL, IMAP_NAME_JOB_CONFIG, IMAP_NAME_MYSQL_SLF4J,
                    IMAP_NAME_PRODUCTS,
                    IMAP_NAME_PERSPECTIVE, IMAP_NAME_PYTHON_SENTIMENT,
                    IMAP_NAME_TRANSACTIONS);

    public static final List<String> IMAP_NAMES_PAYMENTS =
            List.of(IMAP_NAME_AGGREGATE_QUERY_RESULTS, IMAP_NAME_ALERTS_LOG,
                    IMAP_NAME_BICS,
                    IMAP_NAME_JOB_CONTROL, IMAP_NAME_JOB_CONFIG, IMAP_NAME_MYSQL_SLF4J,
                    IMAP_NAME_PERSPECTIVE, IMAP_NAME_PYTHON_SENTIMENT,
                    IMAP_NAME_TRANSACTIONS, IMAP_NAME_TRANSACTIONS_XML);

    public static final List<String> IMAP_NAMES_TRADES =
            List.of(IMAP_NAME_AGGREGATE_QUERY_RESULTS, IMAP_NAME_ALERTS_LOG,
                    IMAP_NAME_JOB_CONTROL, IMAP_NAME_JOB_CONFIG, IMAP_NAME_MYSQL_SLF4J,
                    IMAP_NAME_PERSPECTIVE, IMAP_NAME_PYTHON_SENTIMENT,
                    IMAP_NAME_SYMBOLS, IMAP_NAME_TRANSACTIONS);

    // Maps that are replicated over WAN in enterprise only. Avoid clashing with
    // CommonIdempotentInitialization.java configures some maps with map stores and journals
    public static final List<String> WAN_IMAP_NAMES_ECOMMERCE =
            List.of(IMAP_NAME_AUDIT_LOG, IMAP_NAME_JOB_CONFIG, IMAP_NAME_PRODUCTS);
    public static final List<String> WAN_IMAP_NAMES_PAYMENTS =
            List.of(IMAP_NAME_AUDIT_LOG, IMAP_NAME_JOB_CONFIG, IMAP_NAME_BICS);
    public static final List<String> WAN_IMAP_NAMES_TRADE =
            List.of(IMAP_NAME_AUDIT_LOG, IMAP_NAME_JOB_CONFIG, IMAP_NAME_SYMBOLS);

    public static final String PN_UPDATER = "updater-" + PerspectiveTrade.class.getSimpleName();

    public static final String WEBSOCKET_PATH_TRANSACTIONS = "/transactions";

    // For demonstration of queries
    public static final String SQL_JOB_NAME_IMAP_TO_KAFKA = "imap2Kafka";
    public static final String SQL_JOB_NAME_KAFKA_TO_IMAP = "kafka2IMap";
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
    public static final long TIERED_STORE_DISK_CAPACITY_GB = 6;
    public static final long TIERED_STORE_MEMORY_CAPACITY_MB = 256;

    // For exponential loggers
    public static final int MAX_LOGGING_INTERVAL = 3_000;
}
