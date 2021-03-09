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

package com.hazelcast.platform.demos.banking.trademonitor;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>De-serialize binary data into a {@link Trade} object.
 * </p>
 * <p>Use <a href="https://github.com/FasterXML/jackson-databind/wiki">Jackson</a>
 * libraries to do the JSON parsing.
 * </p>
 */
public class TradeJsonDeserializer implements Deserializer<Trade> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeJsonDeserializer.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * <p>This method is invoked when data comes in from Kafka,
     * "{@code null}" really shouldn't be there.
     * </p>
     */
    @Override
    public Trade deserialize(String topic, byte[] data) {
        if (data == null) {
            LOGGER.error("Null data from topic '{}'", topic);
            return null;
        }

        try {
            return objectMapper.readValue(data, Trade.class);
        } catch (Exception exception) {
            throw new SerializationException(exception);
        }
     }

}
