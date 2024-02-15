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

import java.util.Collection;
import java.util.Set;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>JSON payloads required from more than one class.
 * </p>
 */
@Slf4j
public class MyLocalUtils {

    /**
     * <p>Derive the cluster name to send to a websocket
     * </p>
     *
     * @return
     */
    public static String clusterNamePayload() {
        String clusterName = "";
        int count = 0;

        try {
            // There should be exactly one
            Collection<HazelcastInstance> clients = HazelcastClient.getAllHazelcastClients();
            for (HazelcastInstance client : clients) {
                if (client.getLifecycleService().isRunning()) {
                    clusterName = MyLocalUtils.getClusterName(client);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("clusterNamePayload()", e);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ ");
        if (count == 1 && clusterName.length() > 0) {
            stringBuilder.append(" \"clusterName\": \"" + clusterName + "\"");
        } else {
            stringBuilder.append(" \"clusterName\": \"" + "" + "\"");
            stringBuilder.append(", \"count\": \"" + count + "\"");
        }
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    /**
     * <p>Find the name of the cluster, saved by config. No easier way,
     * see <a href="https://github.com/hazelcast/hazelcast/issues/19294">Issue-19294</a>
     * </p>
     *
     * @param client
     * @return
     */
    private static String getClusterName(HazelcastInstance client) {
        Set<Member> memberSet = client.getCluster().getMembers();
        for (Member member : memberSet) {
            return member.getAttribute(MyConstants.CONFIG_MAP_KEY_CLUSTER_NAME);
        }
        return "";
    }

    /**
     * <p>Cluster name and members.</p>
     * <p>TODO: Duplicates a lot of {@link #clusterNamePayload}, should merge.
     * </p>
     */
    public static String clusterMembersPayload() {
        String clusterName = "";
        String[] memberAddresses = null;
        int count = 0;

        try {
            // There should be exactly one
            Collection<HazelcastInstance> clients = HazelcastClient.getAllHazelcastClients();
            for (HazelcastInstance client : clients) {
                if (client.getLifecycleService().isRunning()) {
                    clusterName = MyLocalUtils.getClusterName(client);
                    count++;
                    Set<Member> memberSet = client.getCluster().getMembers();
                    if (memberSet.size() > 0) {
                        memberAddresses = new String[memberSet.size()];
                        Object[] memberArr = memberSet.toArray();
                        for (int i = 0 ; i < memberArr.length ; i++) {
                            Address address = ((Member) memberArr[i]).getAddress();
                            memberAddresses[i] = address.getHost() + ":" + address.getPort();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("clusterMembersPayload()", e);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ ");
        if (count == 1 && clusterName.length() > 0) {
            stringBuilder.append(" \"clusterName\": \"" + clusterName + "\"");
        } else {
            stringBuilder.append(" \"clusterName\": \"" + "" + "\"");
            stringBuilder.append(", \"count\": \"" + count + "\"");
        }
        stringBuilder.append(", \"members\": [");
        if (memberAddresses != null) {
            for (int i = 0 ; i < memberAddresses.length ; i++) {
                if (i > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append("\"" + memberAddresses[i] + "\"");
            }
        }
        stringBuilder.append("]");
        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

}
