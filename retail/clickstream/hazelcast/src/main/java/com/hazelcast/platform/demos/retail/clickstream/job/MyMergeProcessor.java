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

import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Merge two streams, with logging
 * </p>
 */
@Slf4j
public class MyMergeProcessor extends AbstractProcessor {

    /**
     * <p>Input slot 0 is for models. Rare so log it.
     * </p>
     */
    @Override
    protected boolean tryProcess0(Object item) {
        log.debug("{} emit {}", System.identityHashCode(this), MyUtils.truncateToString(item));
        return super.tryEmit(item);
    }

    /**
     * <p>Input slot 1 is for data. Frequent so don't log it.
     * </p>
     */
    @Override
    protected boolean tryProcess1(Object item) {
        return super.tryEmit(item);
    }
}
