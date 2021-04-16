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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;

/**
 * <p>A listener on cluster change events to report CPU capacity
 * across the cluster to the console.
 * </p>
 */
public class MyMembershipListener implements MembershipListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyMembershipListener.class);

    private final HazelcastInstance hazelcastInstance;

    MyMembershipListener(final HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

    /**
     * <p>Report scale up.</p>
     */
    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        this.report("increased", membershipEvent);
    }

    /**
     * <p>Report scale down.</p>
     */
    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        this.report("decreased", membershipEvent);
    }

    /**
     * <p>Size has changed, report processing capacity, CPUs x nodes.</p>
     * <p>Use "{@code Set<Member>"} as base, in case more members join while
     * running this method.</p>
     */
    private void report(String description, MembershipEvent membershipEvent) {
        List<Member> members = new ArrayList<>(this.hazelcastInstance.getCluster().getMembers());
        int processorsExpected = -1;
        int processorsCount = 0;
        boolean allProcessorsTheSame = true;

        CountProcessorsCallable countProcessorsCallable = new CountProcessorsCallable();
        IExecutorService iExecutorService = this.hazelcastInstance.getExecutorService("default");

        @SuppressWarnings("unchecked")
        int[] processors = new int[members.size()];
        Future<Integer>[] futures = new Future[members.size()];
        for (int i = 0 ; i < members.size(); i++) {
            futures[i] = iExecutorService.submitToMember(countProcessorsCallable, members.get(i));
        }
        for (int i = 0 ; i < members.size(); i++) {
            try {
                processors[i] = futures[i].get();
                if (processorsExpected < 0) {
                    processorsExpected = processors[i];
                }
                if (processors[i] != processorsExpected) {
                    allProcessorsTheSame = false;
                }
                processorsCount += processors[i];
            } catch (Exception e) {
                LOGGER.warn("Abandon reporting, exception on member {}: {}",
                        members.get(i), e.getMessage());
                return;
            }
        }

        LOGGER.info("-------------------------------------");
        LOGGER.info("Cluster {} to {} members", description, members.size());
        LOGGER.info("-------------------------------------");
        for (int i = 0 ; i < members.size(); i++) {
            LOGGER.info(" {}:{} - {} processor{}",
                    members.get(i).getAddress().getHost(), members.get(i).getAddress().getPort(),
                    processors[i],
                    (processors[i] == 1 ? "" : "s"));
        }
        if (!allProcessorsTheSame) {
            LOGGER.info("-------------------------------------");
            LOGGER.warn("Processor count is not same across all nodes");
        }
        LOGGER.info("-------------------------------------");
        LOGGER.info("Total processors {}", processorsCount);
        LOGGER.info("-------------------------------------");
    }

}
