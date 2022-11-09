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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>De-serialize binary data into a {@link Transaction} object.
 * </p>
 * <p>Use <a href="https://github.com/FasterXML/jackson-databind/wiki">Jackson</a>
 * libraries to do the JSON parsing.
 * </p>
 */
public class TransactionJsonDeserializer implements Deserializer<Transaction> {
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * <p>This method is invoked when data comes in from Kafka,
     * "{@code null}" really shouldn't be there.
     * </p>
     */
    @Override
    public Transaction deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, Transaction.class);
        } catch (Exception exception) {
            throw new SerializationException(exception);
        }
     }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

}
