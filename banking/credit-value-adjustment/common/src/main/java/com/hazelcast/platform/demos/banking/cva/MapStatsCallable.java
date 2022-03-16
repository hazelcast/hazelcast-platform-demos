/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.banking.cva;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.map.LocalMapStats;
import com.hazelcast.nearcache.NearCacheStats;

/**
 * <p>A task to run across all server nodes in a cluster, to return
 * the local counts from each for global summation.
 * </p>
 */
public class MapStatsCallable implements Callable<HazelcastJsonValue>, HazelcastInstanceAware, Serializable {

    private static final long serialVersionUID = 1L;
    private final String mapName;
    private transient HazelcastInstance hazelcastInstance;

    public MapStatsCallable(String arg0) {
        this.mapName = arg0;
        // See comment on setHazelcastInstance()
        this.hazelcastInstance = null;
    }

    /**
     * <p>Set on called node by Hazelcast prior to execution.</p>
     * <p><a href="https://spotbugs.github.io/">SpotBugs</a> will
     * complain if transient field "{@code hazelcastInstance}" is
     * not set in the constructor, as it is not aware that Hazelcast
     * will call "{@code setHazelcastInstance()}" before "{@code call()}".
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

    /**
     * <p>Find the count of selected operations for the given map.</p>
     */
    @Override
    public HazelcastJsonValue call() throws Exception {
        IMap<?, ?> iMap = this.hazelcastInstance.getMap(mapName);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ ");

        LocalMapStats localMapStats = iMap.getLocalMapStats();
        if (localMapStats != null) {
            stringBuilder.append(" \"reads\": " + localMapStats.getGetOperationCount());
            stringBuilder.append(", \"writes\": " + (localMapStats.getPutOperationCount()
                    + localMapStats.getSetOperationCount()));
            stringBuilder.append(", \"deletes\": " + localMapStats.getRemoveOperationCount());

            NearCacheStats nearCacheStats = localMapStats.getNearCacheStats();
            if (nearCacheStats != null) {
                stringBuilder.append(", \"near_cache\": {");
                stringBuilder.append("  \"hits\": " + nearCacheStats.getHits());
                stringBuilder.append(", \"misses\": " + nearCacheStats.getMisses());
                stringBuilder.append(" }");
            }
        }

        stringBuilder.append(" }");
        return new HazelcastJsonValue(stringBuilder.toString());
    }

}
