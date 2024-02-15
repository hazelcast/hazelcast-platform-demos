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

package com.hazelcast.platform.demos.banking.cva;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.map.IMap;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;

/**
 * <p>A task to run across all server nodes in a cluster, to return
 * counts of keys per partition on each member.
 * </p>
 * <p>Each member will return something like:
 * <pre>
 *  127.0.0.1:5701, [ [5, 10], [6, 12] ]
 * </pre>
 * to indicate the member "{@code 127.0.0.1:5701}" has partition 5 with
 * 10 entries and partition 6 with 12 entries. So, 22 primary data copies
 * of entries for the given map<p>
 */
public class MapMemberPartitionCallable
    implements Callable<Tuple2<String, Map<Integer, Long>>>,
        HazelcastInstanceAware, Serializable {

    private static final long serialVersionUID = 1L;
    private final String mapName;
    private transient HazelcastInstance hazelcastInstance;

    public MapMemberPartitionCallable(String arg0) {
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
     * <p>Find the sizes per partition for a map on this member.</p>
     */
    @Override
    public Tuple2<String, Map<Integer, Long>> call() throws Exception {
        IMap<?, ?> iMap = this.hazelcastInstance.getMap(mapName);
        PartitionService partitionService = this.hazelcastInstance.getPartitionService();

        String name = buildName(this.hazelcastInstance.getCluster());

        Map<Integer, Long> counts = new HashMap<>();
        iMap.localKeySet().forEach(key -> {
            Partition partition = partitionService.getPartition(key);
            counts.merge(partition.getPartitionId(), 1L, Long::sum);
        });

        return Tuple2.tuple2(name, counts);
    }


    /**
     * <p>Return the member's place in the membership list, and IP.
     * So "{@code 005,127.0.0.1:5706}" to indicate this is sixth (5 from 0)
     * and the host/port pair. Do it this way so it can be sorted.
     * </p>
     *
     * @param cluster
     * @return
     */
    private String buildName(Cluster cluster) {
        DecimalFormat decimalFormat = new DecimalFormat("000");
        String name = "";

        Member localMember = cluster.getLocalMember();

        // Where is this member in the list of members
        Iterator<Member> membersIterator = cluster.getMembers().iterator();
        int ordinal = 0;
        while (membersIterator.hasNext()) {
            Member candidateMember = membersIterator.next();
            if (localMember.getUuid().equals(candidateMember.getUuid())) {
                name = decimalFormat.format(ordinal);
            }
            ordinal++;
        }
        name += ",";

        // IP Address
        Address address = this.hazelcastInstance.getCluster().getLocalMember().getAddress();
        name += address.getHost() + ":" + address.getPort();

        return name;
    }

}
