/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.map.IMap;
import com.hazelcast.map.LocalMapStats;

/**
 * <p>A task to run across all server nodes in a cluster, to return
 * the local counts from each for global summation.
 * </p>
 */
public class MapStatsCallable implements Callable<MapStatsCallable.MyMapStats>, HazelcastInstanceAware, Serializable {

    private static final long serialVersionUID = 1L;
    private final String mapName;
    private HazelcastInstance hazelcastInstance;

    public MapStatsCallable(String arg0) {
        this.mapName = arg0;
    }

    /**
     * <p>Set on called node by Hazelcast prior to execution.</p>
     */
    @Override
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

    /**
     * <p>Find the count of selected operations for the given map.</p>
     */
    @Override
    public MyMapStats call() throws Exception {
        IMap<?, ?> iMap = this.hazelcastInstance.getMap(mapName);

        LocalMapStats localMapStats = iMap.getLocalMapStats();

        if (localMapStats != null) {
            MyMapStats myMapStats = new MyMapStats();

            myMapStats.setRead(localMapStats.getGetOperationCount());
            myMapStats.setWrite(localMapStats.getPutOperationCount() + localMapStats.getSetOperationCount());
            myMapStats.setDelete(localMapStats.getRemoveOperationCount());

            return myMapStats;
        }

        return null;
    }

    /** <p>A convenience internal class for the return
     * object.</p>
     */
    public class MyMapStats {
        private long read;
        private long write;
        private long delete;

        /** <p>Generated</p> */
        public long getRead() {
            return read;
        }
        /** <p>Generated</p> */
        public void setRead(long read) {
            this.read = read;
        }
        /** <p>Generated</p> */
        public long getWrite() {
            return write;
        }
        /** <p>Generated</p> */
        public void setWrite(long write) {
            this.write = write;
        }
        /** <p>Generated</p> */
        public long getDelete() {
            return delete;
        }
        /** <p>Generated</p> */
        public void setDelete(long delete) {
            this.delete = delete;
        }
    }

}
