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

package com.hazelcast.platform.demos.retail.clickstream.job;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.map.IMap;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>A batch source for a job that reads a single entry's value from
 * a map for a given key.
 * </p>
 */
@Slf4j
public class MapValueSource {
    private final HazelcastInstance hazelcastInstance;
    private final Object key;
    private final String mapName;

    public MapValueSource(HazelcastInstance arg0, String arg1, Object arg2) {
        this.hazelcastInstance = arg0;
        this.mapName = arg1;
        this.key = arg2;
    }

    public void fillBufferFn(SourceBuilder.SourceBuffer<Object> buffer) {
        IMap<Object, ?> map = this.hazelcastInstance.getMap(this.mapName);
        Object value = map.get(this.key);
        if (value == null) {
            log.error("map'{}' get('{}') -> no data found",
                    map.getName(),
                    this.key);
        } else {
            buffer.add(value);
            log.trace("map'{}' get('{}') -> value toString() size {} bytes",
                    map.getName(),
                    this.key,
                    value.toString().length());
        }
        // No more input, end of batch
        buffer.close();
    }

}
