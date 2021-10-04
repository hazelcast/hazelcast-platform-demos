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

package com.hazelcast.platform.demos.retail.clickstream;

import java.util.List;

/**
 * <p>Constants for the app.
 * </p>
 */
public class MyConstants {

    // 271 is default partition count
    public static final int JOURNAL_CAPACITY = 271 * 10_000;
    public static final String KUBERNETES_PROPERTY_SERVICE_DNS = "service-dns";
    public static final String PERSISTENCE_BASEDIR = "basedir";

    // Map names, for eager creation
    public static final String IMAP_NAME_ALERT = "sys.alert";
    public static final String IMAP_NAME_CHECKOUT = "checkout";
    public static final String IMAP_NAME_CLICKSTREAM = "clickstream";
    public static final String IMAP_NAME_CONFIG  = "sys.config";
    public static final String IMAP_NAME_DIGITAL_TWIN = "digital_twin";
    public static final String IMAP_NAME_HEARTBEAT = "sys.heartbeat";
    public static final String IMAP_NAME_MODEL_SELECTION = "model_selection";
    public static final String IMAP_NAME_MODEL_VAULT = "model_vault";
    public static final String IMAP_NAME_ORDERED = "ordered";
    public static final String IMAP_NAME_RETRAINING_ASSESSMENT = "retraining_assessment";
    public static final String IMAP_NAME_RETRAINING_CONTROL = "retraining_control";
    public static final String IMAP_NAME_PREDICTION = "prediction";
    public static final List<String> IMAP_NAMES =
            List.of(IMAP_NAME_ALERT, IMAP_NAME_CHECKOUT, IMAP_NAME_CLICKSTREAM,
                    IMAP_NAME_CONFIG, IMAP_NAME_DIGITAL_TWIN, IMAP_NAME_HEARTBEAT,
                    IMAP_NAME_MODEL_SELECTION, IMAP_NAME_MODEL_VAULT, IMAP_NAME_ORDERED,
                    IMAP_NAME_RETRAINING_ASSESSMENT, IMAP_NAME_RETRAINING_CONTROL, IMAP_NAME_PREDICTION);

    // Try not to differ from map name
    public static final String CASSANDRA_TABLE_NAME_MODEL = "model";

    public static final String PULSAR_TOPIC = "feed";

    // For "pulsar_feed"
    public static final String CSV_INPUT_FILE1 = "testing_sample.csv";
    public static final String CSV_INPUT_FILE2 = "training_sample.csv";

    // For binary backoff loggers
    public static final int MAX_LOGGING_INTERVAL = 10_000;
    public static final int MAX_LOGGING_LINE_LENGTH = 120;

    // Graphite/Grafana sink
    public static final int GRAPHITE_PORT = 2004;

    // WAN config, default batch 500, queue 10000
    public static final int WAN_BATCH_SIZE = 1_000;
    public static final int WAN_QUEUE_SIZE = 50_000;
    // Locally cached config
    public static final String CONFIG_MAP_KEY_CLUSTER_NAME = "clusterName";
    public static final String CONFIG_MAP_KEY_GRAPHITE = "graphite";

    public static final int RETRAINING_INTERVAL = 10_000;
    public static final long VALIDATION_GRACE_PERIOD = 30L;
    public static final String RETRAINING_CONTROL_ACTION_RETRAIN = "retrain";
    public static final String RETRAINING_CONTROL_ACTION_VALIDATE = "validate";
}
