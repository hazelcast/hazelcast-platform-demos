/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import com.hazelcast.jet.accumulator.LongAccumulator;
import com.hazelcast.jet.core.AbstractProcessor;

/**
 * <p>Part of exponential logging. Format and emit an error
 * message for the next job stage to print.
 * </p>
 */
public class FilterFormatterProcessor extends AbstractProcessor {

    private long cap;
    private LongAccumulator counter = new LongAccumulator(0);
    private Long currentThreshold = Long.valueOf(1L);

    public FilterFormatterProcessor(long arg0) {
        this.cap = arg0;
    }

    protected boolean tryProcess(int ordinal, Object item) {
        this.counter.add(1L);
        if ((this.counter.get() < this.cap) && (this.counter.get() % this.currentThreshold == 0)) {
            if (2 * currentThreshold <= MyConstants.MAX_LOGGING_INTERVAL) {
                this.currentThreshold = Long.valueOf(2 * this.currentThreshold);
            }
            String message = String.format("Item %d -> '%s'", this.counter.get(), MyUtils.truncateToString(item));
            return super.tryEmit(message);
        }
        return true;
    }
}
