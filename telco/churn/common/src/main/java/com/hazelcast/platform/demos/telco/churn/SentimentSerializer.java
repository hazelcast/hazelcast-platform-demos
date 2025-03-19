/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import com.hazelcast.platform.demos.telco.churn.domain.Sentiment;

/**
 * <p>Serializer for {@link Sentiment}
 * </p>
 */
public class SentimentSerializer implements CompactSerializer<Sentiment> {

    @Override
    public Class<Sentiment> getCompactClass() {
        return Sentiment.class;
    }

    @Override
    public String getTypeName() {
        return Sentiment.class.getName();
    }

    @Override
    public Sentiment read(CompactReader reader) {
        Sentiment sentiment = new Sentiment();
        long timestamp = reader.readInt64("updated");
        sentiment.setUpdated(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
        sentiment.setCurrent(reader.readFloat64("current"));
        sentiment.setPrevious(reader.readFloat64("previous"));
        return sentiment;
    }

    @Override
    public void write(CompactWriter writer, Sentiment target) {
        long timestamp = target.getUpdated().atZone(ZoneId.systemDefault()).toEpochSecond();
        writer.writeInt64("updated", timestamp);
        writer.writeFloat64("current", target.getCurrent());
        writer.writeFloat64("previous", target.getPrevious());
    }

}
