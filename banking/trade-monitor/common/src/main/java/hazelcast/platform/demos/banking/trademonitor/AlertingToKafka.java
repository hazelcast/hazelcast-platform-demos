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

package hazelcast.platform.demos.banking.trademonitor;

import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.kafka.KafkaSinks;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sources;

/**
 * <p>Propagate alerts from the {@link IMap} to Kafka egest.
 * </p>
 */
public class AlertingToKafka {

    /**
     * <p>Read from the journal, write to the topic. A {@link Pipeline}
     * currently.
     * TODO: Convert to streaming SQL "{@code SINK INTO kafka SELECT * FROM map}"
     * once 5.3 available.
     * </p>
     *
     * @param bootstrapServers Connection list for Kafka
     * @return A pipeline job to run in Jet.
     */
    public static Pipeline buildPipeline(String bootstrapServers) {

        // Override the value de-serializer to produce a different type
        Properties properties = InitializerConfig.kafkaSourceProperties(bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, HazelcastJsonValueSerializer.class.getName());

        return Pipeline.create()
        .readFrom(Sources.<Long, HazelcastJsonValue>mapJournal(MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME,
                JournalInitialPosition.START_FROM_OLDEST))
                .withoutTimestamps()
        .writeTo(KafkaSinks.kafka(properties, MyConstants.KAFKA_TOPIC_NAME_ALERTS))
        .getPipeline();
    }

}
