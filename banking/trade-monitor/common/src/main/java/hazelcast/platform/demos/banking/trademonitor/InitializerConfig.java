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
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * <p>Config used by clientside and serverside initialization
 * </p>
 */
public class InitializerConfig {

    /**
     * <p>Control properties for the Kafka connector.
     * </p>
     * <ul>
     * <li><p><i>AUTO_OFFSET_RESET_CONFIG</i> - where to begin reading from.
     * <p></li>
     * <li><p><i>BOOTSTRAP_SERVERS_CONFIG</i> - the list of brokers to connect to.
     * <p></li>
     * <li><p><i>GROUP_ID_CONFIG</i> - The Id for the Jet job connecting to Kafka,
     * make it unique rather than rely on Kafka generating one.
     * <p></li>
     * <li><p><i>KEY_DESERIALIZER_CLASS_CONFIG</i> - how to de-serialize the message key.
     * <p></li>
     * <li><p><i>VALUE_DESERIALIZER_CLASS_CONFIG</i> - how to de-serialize the message value.
     * <p></li>
     * </ul>
     *
     * @param bootstrapServers A CSV list of brokers
     * @return Properties used by the Kafka connector
     */
    public static Properties kafkaSourceProperties(String bootstrapServers) {

        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());

        return properties;
    }

}
