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

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.platform.demos.utils.UtilsFormatter;

/**
 * <p>Accumulate and output a maximum value.
 * </p>
 */
public class MaxVolumeAggregator implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(MaxVolumeAggregator.class);

    private String maxSymbol;
    private long maxVolume;

    public static AggregateOperation1<Entry<Integer, Tuple4<String, Long, Long, Long>>,
        MaxVolumeAggregator, Entry<Long, HazelcastJsonValue>> buildMaxVolumeAggregation() {
        return AggregateOperation
                .withCreate(MaxVolumeAggregator::new)
                .andAccumulate((MaxVolumeAggregator maxVolumeAggregator,
                        Entry<Integer, Tuple4<String, Long, Long, Long>> entry)
                        -> maxVolumeAggregator.accumulate(entry.getValue()))
                .andCombine(MaxVolumeAggregator::combine)
                .andExportFinish(MaxVolumeAggregator::exportFinish);
    }

    /**
     * <p>Update the max if necessary.
     * </p>
     *
     * @param entry
     * @return The current accumulator
     */
    public MaxVolumeAggregator accumulate(Tuple4<String, Long, Long, Long> tuple4) {
        if (this.maxSymbol == null) {
            this.maxSymbol = tuple4.f0();
            this.maxVolume = tuple4.f2();
        } else {
            if (this.maxVolume < tuple4.f2()) {
                this.maxSymbol = tuple4.f0();
                this.maxVolume = tuple4.f2();
            }
        }
        return this;
    }

    /**
     * <p>Update the max if necessary.
     * </p>
     */
    public MaxVolumeAggregator combine(MaxVolumeAggregator that) {
        String thatMaxSymbol = that.getMaxSymbol();
        Long thatMaxVolume = that.getMaxVolume();
        if (this.maxSymbol == null) {
            // May be null on merging value also
            this.maxSymbol = thatMaxSymbol;
            this.maxVolume = thatMaxVolume;
        } else {
            if (thatMaxSymbol != null && this.maxVolume < thatMaxVolume) {
                this.maxSymbol = thatMaxSymbol;
                this.maxVolume = thatMaxVolume;
            }
        }
        return this;
    }

    /**
     * <p>For combining, need access to maximum as getter().
     * </p>
     *
     * @return
     */
    private String getMaxSymbol() {
        return this.maxSymbol;
    }
    private Long getMaxVolume() {
        return this.maxVolume;
    }

    /**
     * <p>Format result
     * </p>
     */
    public Entry<Long, HazelcastJsonValue> exportFinish() {
        long now = System.currentTimeMillis();
        String nowStr = UtilsFormatter.timestampToISO8601(now);
        if (this.maxSymbol == null) {
            LOGGER.error("exportFinish() : this.max == null");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            stringBuilder.append("  \"symbol\" : \"<none>\"");
            stringBuilder.append(", \"timestamp\" : \"" + nowStr + "\"");
            stringBuilder.append(", \"volume\" : 0");
            stringBuilder.append("}");
            Entry<Long, HazelcastJsonValue> entry =
                    new SimpleImmutableEntry<>(now, new HazelcastJsonValue(stringBuilder.toString()));
            return entry;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            stringBuilder.append("  \"symbol\" : \"" + this.maxSymbol + "\"");
            stringBuilder.append(", \"timestamp\" : \"" + nowStr + "\"");
            stringBuilder.append(", \"volume\" : " + this.maxVolume);
            stringBuilder.append("}");
            Entry<Long, HazelcastJsonValue> entry =
                    new SimpleImmutableEntry<>(now, new HazelcastJsonValue(stringBuilder.toString()));
            return entry;
        }
    }
}
