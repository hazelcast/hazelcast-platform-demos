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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.pipeline.Pipeline;

/**
 * FIXME Placeholder to add Debezium ingest for Mongo
 */
public class MongoDebeziumOneWayCDC extends MyJobWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDebeziumOneWayCDC.class);

    private String myMongo;

    MongoDebeziumOneWayCDC(long timestamp) {
        super(timestamp);

        // Configure expected MySql address for Docker or Kubernetes
        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            this.myMongo =
                 System.getProperty("my.project") + "-mysql.default.svc.cluster.local";

            LOGGER.info("Kubernetes configuration: mysql host: '{}'", this.myMongo);
        } else {
            this.myMongo = "mongo";
            LOGGER.info("Non-Kubernetes configuration: mysql host: '{}'", this.myMongo);
        }
    }

    /**
     * <p>Create the pipeline.
     * </p>
     */
    public Pipeline getPipeline() {
        Pipeline pipeline = Pipeline.create();

        return pipeline;
    }
}
