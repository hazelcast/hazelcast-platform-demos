/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

import java.util.List;

/**
 * <p>Utility constants shared across the modules.
 * </p>
 */
public class MyConstants {

    // Used also in Node.js clients, Java clients and Java servers.
    public static final int CLASS_ID_MYDATASERIALIZABLEFACTORY = 1000;
    public static final int CLASS_ID_MYCREDENTIALS = 1;

    // Map names, for eager creation
    public static final String IMAP_NAME_CDR  = "cdr";
    public static final String IMAP_NAME_CUSTOMER  = "customer";
    public static final String IMAP_NAME_TARIFF  = "tariff";
    public static final List<String> IMAP_NAMES =
            List.of(IMAP_NAME_CDR, IMAP_NAME_CUSTOMER,
                    IMAP_NAME_TARIFF);

    // Topic names, for eager creation
    public static final String ITOPIC_NAME_SLACK  = "slack";
    public static final List<String> ITOPIC_NAMES =
            List.of(ITOPIC_NAME_SLACK);

    // For Jet job that writes to Slack
    public static final String SLACK_ACCESS_TOKEN = "accessToken";
    public static final String SLACK_CHANNEL_ID = "channelId";
    public static final String SLACK_CHANNEL_NAME = "channelName";

    // To read from Kafka, must match Dockerfile in topic-create module
    public static final String KAFKA_TOPIC_CALLS_NAME = "calls";
    public static final int KAFKA_TOPIC_CALLS_PARTITIONS = 3;
    // Read from "cassandra" topic, not write, so don't need to know partition count
    // See debezium-connector-cassandra.conf and topic-create/Dockerfile
    public static final String KAFKA_TOPIC_CASSANDRA = "debezium.churn.cdr";

    // Backing store names, Mongo is derived, others need specified
    public static final String CASSANDRA_TABLE_NAME = "cdr";
    public static final String MYSQL_SCHEMA_NAME = "churn";
    public static final String MYSQL_TABLE_NAME = "tariff";
}
