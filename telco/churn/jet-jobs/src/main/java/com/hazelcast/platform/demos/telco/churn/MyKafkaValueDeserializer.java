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

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.serialization.Deserializer;

import com.hazelcast.core.HazelcastJsonValue;

/**
 * <p>A deserializer that can handle {@link HazelcastJsonValue} from a String.
 * </p>
 */
public class MyKafkaValueDeserializer implements Deserializer<HazelcastJsonValue> {

    @Override
    public HazelcastJsonValue deserialize(String topic, byte[] data) {
        return new HazelcastJsonValue(new String(data, StandardCharsets.UTF_8));
    }

}
