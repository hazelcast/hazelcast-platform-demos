/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.platform.demos.utils.UtilsFormatter;

/**
 * <p>Accumulate and output a maximum value.
 * </p>
 */
public class MaxAggregator implements Serializable {
    private static final long serialVersionUID = 1L;
    // Don't log, if running in Viridian user may not download logs
    // private static final Logger LOGGER = LoggerFactory.getLogger(MaxAggregator.class);

    private final String provenance;
    private String maxSymbol;
    private double maxVolume;

    public MaxAggregator(String arg0, String arg1, String arg2) {
        this.provenance = arg0 + ":" + arg1 + ":" + arg2;
    }

    public static AggregateOperation1<Entry<Integer, Tuple4<String, Long, Double, Double>>,
        MaxAggregator, Entry<Long, HazelcastJsonValue>> buildMaxAggregation(
                String projectName, String clusterName, String jobName
                ) {
        return AggregateOperation
                .withCreate(() -> new MaxAggregator(projectName, clusterName, jobName))
                .andAccumulate((MaxAggregator maxVolumeAggregator,
                        Entry<Integer, Tuple4<String, Long, Double, Double>> entry)
                        -> maxVolumeAggregator.accumulate(entry.getValue()))
                .andCombine(MaxAggregator::combine)
                .andExportFinish(MaxAggregator::exportFinish);
    }

    /**
     * <p>Update the max if necessary.
     * </p>
     *
     * @param entry
     * @return The current accumulator
     */
    public MaxAggregator accumulate(Tuple4<String, Long, Double, Double> tuple4) {
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
    public MaxAggregator combine(MaxAggregator that) {
        String thatMaxSymbol = that.getMaxSymbol();
        Double thatMaxVolume = that.getMaxVolume();
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
    private Double getMaxVolume() {
        return this.maxVolume;
    }

    /**
     * <p>Format result. Format needs to match or at least correspond with
     * Postgres database definition, and with mapping in {@link TransactionMonitorIdempotentInitialization}.
     * and {@link PostgresCDC}
     * </p>
     */
    public Entry<Long, HazelcastJsonValue> exportFinish() {
        long now = System.currentTimeMillis();
        String nowStr = UtilsFormatter.timestampToISO8601(now);
        if (this.maxSymbol == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            stringBuilder.append("  \"code\" : \"<none>\"");
            stringBuilder.append(", \"provenance\" : \"" + this.provenance + "\"");
            stringBuilder.append(", \"whence\" : \"" + nowStr + "\"");
            stringBuilder.append(", \"volume\" : 0");
            stringBuilder.append("}");
            Entry<Long, HazelcastJsonValue> entry =
                    new SimpleImmutableEntry<>(now, new HazelcastJsonValue(stringBuilder.toString()));
            return entry;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            stringBuilder.append("  \"code\" : \"" + this.maxSymbol + "\"");
            stringBuilder.append(", \"provenance\" : \"" + this.provenance + "\"");
            stringBuilder.append(", \"whence\" : \"" + nowStr + "\"");
            stringBuilder.append(", \"volume\" : " + this.maxVolume);
            stringBuilder.append("}");
            Entry<Long, HazelcastJsonValue> entry =
                    new SimpleImmutableEntry<>(now, new HazelcastJsonValue(stringBuilder.toString()));
            return entry;
        }
    }
}
