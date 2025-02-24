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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.map.MapStore;

/**
 * <p>A map store that stores to the logs, write-only.
 * </p>
 */
public class TodayLoggingMapStore implements MapStore<String, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TodayLoggingMapStore.class);

    private final String prefix;

    TodayLoggingMapStore(String arg0) {
        this.prefix = arg0;
    }

    @Override
    public String load(String key) {
        LOGGER.error("**{}**'{}'::load('{}') -> unexpected", LocalConstants.MY_JAR_NAME, this.prefix, key);
        return null;
    }

    @Override
    public Map<String, String> loadAll(Collection<String> keys) {
        LOGGER.error("**{}**'{}'::loadAll('{}') -> unexpected", LocalConstants.MY_JAR_NAME, this.prefix, keys);
        return null;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        LOGGER.trace("**{}**'{}'::loadAllKeys() -> []", LocalConstants.MY_JAR_NAME, this.prefix);
        return Collections.emptyList();
    }

    @Override
    public void store(String key, String value) {
        LOGGER.info("**{}**'{}'::store('{}') -> '{}'", LocalConstants.MY_JAR_NAME, this.prefix, key, value);
    }

    /**
     * <p>Unexpected as will not be configured for write-behind.
     * </p>
     */
    @Override
    public void storeAll(Map<String, String> map) {
        LOGGER.error("**{}**'{}'::storeAll('{}') -> unexpected", LocalConstants.MY_JAR_NAME, this.prefix, map);
    }

    @Override
    public void delete(String key) {
        LOGGER.error("**{}**'{}'::delete('{}') -> unexpected", LocalConstants.MY_JAR_NAME, this.prefix, key);
    }

    @Override
    public void deleteAll(Collection<String> keys) {
        LOGGER.error("**{}**'{}'::deleteAll('{}') -> unexpected", LocalConstants.MY_JAR_NAME, this.prefix, keys);
    }

}
