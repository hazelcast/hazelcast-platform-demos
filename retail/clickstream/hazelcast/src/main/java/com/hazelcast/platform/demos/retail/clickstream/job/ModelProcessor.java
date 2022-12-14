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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.map.IMap;
import com.hazelcast.map.MapLoader;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Simple lookup of a model from a map. Map has a
 * {@link MapLoader} so this loads the model from
 * Cassandra.
 * </p>
 */
@Slf4j
public class ModelProcessor extends AbstractProcessor {
    private final IMap<String, String> modelMap;

    public ModelProcessor(HazelcastInstance hazelcastInstance) {
        this.modelMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_MODEL_VAULT);
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) {
        String key = item.toString();
        String value = this.modelMap.get(key);
        if (value == null) {
            log.error("tryProcess({}, '{}') -> null", ordinal, item);
            return true;
        } else {
            log.debug("tryProcess({}, '{}') -> !null, length()=={}", ordinal, item, value.length());
        }
        Entry<String, String> entry = new SimpleImmutableEntry<>(key, value);
        return super.tryEmit(entry);
    }

    /**
     * <p>Non-cooperative, {@link IMap#get} may be slow.
     * </p>
     */
    @Override
    public boolean isCooperative() {
        return false;
    }

}
