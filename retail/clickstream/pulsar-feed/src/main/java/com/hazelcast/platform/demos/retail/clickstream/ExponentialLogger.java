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

import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Log lines 1, 2, 4, 8 ... up to a limit then log in intervals
 * of that limit.
 * </p>
 */
@Slf4j
public class ExponentialLogger {
    private static final int TEN = 10;

    private final String prefix;
    private int binaryLoggingInterval = 1;
    private int count;

    ExponentialLogger(String arg0, String arg1) {
        String tmp = arg1.toUpperCase(Locale.ROOT) + "..........";
        this.prefix = arg0 + ":" + tmp.substring(0, TEN) + ":";
    }

    public void log(int read, int written, String key, String value) {
        if (this.count % this.binaryLoggingInterval == 0) {
            log.debug("{} input line {} write {} -> key '{}', value '{}'", prefix, read, written, key, value);
            if (2 * this.binaryLoggingInterval <= MyConstants.MAX_LOGGING_INTERVAL) {
                this.binaryLoggingInterval = 2 * this.binaryLoggingInterval;
            }
        }
        this.count++;
    }

}
