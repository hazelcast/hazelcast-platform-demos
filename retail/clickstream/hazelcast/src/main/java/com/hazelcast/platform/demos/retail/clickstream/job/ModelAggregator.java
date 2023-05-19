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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Collates all models in a batch, last is best
 * </p>
 */
@Getter
@Slf4j
public class ModelAggregator implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String modelName;
    private String modelStr;
    private long timestamp;
    private int failureCount;

    public ModelAggregator(String arg0) {
        this.modelName = arg0;
    }

    /**
     * <p>Takes some models passed as Strings, and returns the last.
     * </p>
     * @return
     */
    public static AggregateOperation1<String, ModelAggregator, Entry<String, String>> buildModelAggregation(
            String arg0) {
        return AggregateOperation
                .withCreate(() -> new ModelAggregator(arg0))
                .andAccumulate((ModelAggregator modelAggregator, String s)
                        -> modelAggregator.accumulate(s))
                .andCombine(ModelAggregator::combine)
                .andExportFinish(ModelAggregator::exportFinish);
    }

    /**
     * <p>Input format is a comma separated pair.
     * Model is Base64, keep that way until output.
     * </p>
     *
     * @param arg0
     */
    public void accumulate(String arg0) {
        String[] tokens = arg0.split(",");
        if (tokens.length == 2) {
            this.timestamp = Long.parseLong(tokens[0]);
            this.modelStr = tokens[1];
        } else {
            // Check for failure,timestamp, reason
            if (tokens.length == 3 && tokens[0].equals("failure")) {
                if (arg0.contains("len==")) {
                    this.failureCount++;
                    // Only log first few length failures as likely to be recurring
                    if (this.failureCount < 3) {
                        log.debug("accumulate() '{}': Python failure:'{}'",
                                this.modelName, arg0);
                    }
                } else {
                    log.error("accumulate() '{}': Python failure:'{}'",
                            this.modelName, arg0);
                }
            } else {
                log.error("accumulate() '{}': tokens.length=={}", this.modelName, tokens.length);
            }
        }
    }

    /**
     * <p>Keep latest
     * </p>
     *
     * @param that
     */
    public void combine(ModelAggregator that) {
        if (that.getTimestamp() > this.timestamp) {
            this.timestamp = that.getTimestamp();
            this.modelStr = that.getModelStr();
        }
        this.failureCount += that.getFailureCount();
    }

    /**
     * <p>Format result
     * </p>
     *
     * @return
     */
    public Entry<String, String> exportFinish() {
        if (this.timestamp == 0 || this.modelStr == null) {
            log.error("exportFinish() '{}': this.timestamp=={} (this.model==null)=={}",
                    this.modelName, this.timestamp, this.modelStr == null);
            return null;
        }
        return new SimpleImmutableEntry<>(this.modelName, this.modelStr);
    }

}
