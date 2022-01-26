/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.python.PythonTransforms;

import hazelcast.platform.demos.banking.trademonitor.IngestTrades;
import hazelcast.platform.demos.banking.trademonitor.MyConstants;
import hazelcast.platform.demos.banking.trademonitor.MyUtils;

/**
 * <p>A Jet pipeline that streams data from Kakfa through Python into
 * a memory cache. 4 simple steps chained together.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                |  Kafka Source   |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                |  Call Python    |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 3 )------+
 *                |    Reformat     |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 4 )------+
 *                |    IMap Sink    |
 *                +-----------------+
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * Kafka source
 * </p>
 * <p>Read all messages from the beginning from a Kafka topic.
 * </p>
 * </li>
 * <li>
 * <p>
 * Python
 * </p>
 * <p>Pass the JSON read from Kafka into Python. The Python module does
 * something, opaque to this processing, and returns CSV strings.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformat
 * </p>
 * <p>Convert the CSV output from Python into a {@link java.util.Map.Entry}
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
public class PythonAnalysis {

    /**
     * <p>Build the pipeline in a fluent coding style.
     * </p>
     * <p>If you wish to construct in fragments, such as:
     * <pre>
     * Pipeline pipeline = Pipeline.create();
     *
     * pipeline
     *  .readFrom(...)
     *  .do_something(...)
     *  .do_something_else(...)
     *  .writeTo(...);
     *
     * return pipeline;
     * </pre>
     * then see the {@link IngestTrades#buildPipeline(String)}</p>
     * @return
     */
    public static Pipeline buildPipeline(Properties properties, String buildTimestamp) throws Exception {
        return
                Pipeline
                .create()
                .readFrom(KafkaSources.<String, String, String>
                    kafka(properties, ConsumerRecord::value, MyConstants.KAFKA_TOPIC_NAME_TRADES)).withoutTimestamps()
                .apply(PythonTransforms.mapUsingPython(MyUtils.getPythonServiceConfig("slow", "processFn")))
                .map(csv -> {
                    String[] tokens = csv.split(",");
                    return new SimpleImmutableEntry<String, String>(tokens[0] + "@" + buildTimestamp, tokens[1]);
                })
                .writeTo(Sinks.map(MyConstants.IMAP_NAME_PYTHON_SENTIMENT))
                .getPipeline();
    }

}
