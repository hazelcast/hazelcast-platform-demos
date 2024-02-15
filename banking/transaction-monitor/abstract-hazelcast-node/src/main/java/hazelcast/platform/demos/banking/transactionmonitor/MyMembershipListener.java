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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.datamodel.Tuple2;

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

        try {
            LOGGER.info("=====================================");
            this.logPartitions();
            LOGGER.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            this.logJobs();
            LOGGER.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        } catch (Exception e) {
            LOGGER.error("report() -> logPartitions()", e);
        }
        CountProcessorsCallable countProcessorsCallable = new CountProcessorsCallable();
        IExecutorService iExecutorService = this.hazelcastInstance.getExecutorService("default");

        int[] processors = new int[members.size()];
        @SuppressWarnings("unchecked")
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
        LOGGER.info("=====================================");
    }

    /**
     * <p>Assess the loading of the partitions.
     * </p>
     * <p>See <a href="https://hazelcast.com/blog/calculation-in-hazelcast-cloud/">here</a>
     * for a more efficient way to calculate Standard Deviation. Here we go for simplicity.
     * </p>
     */
    private void logPartitions() {
        CountIMapPartitionsCallable countIMapPartitionsCallable = new CountIMapPartitionsCallable();
        final Map<Integer, Tuple2<Integer, String>> collatedResults = new TreeMap<>();

        Map<Member, Future<Map<Integer, Integer>>> rawResults =
                this.hazelcastInstance.getExecutorService("default").submitToAllMembers(countIMapPartitionsCallable);

        rawResults.entrySet()
        .stream()
        .forEach(memberEntry -> {
            try {
                String member = memberEntry.getKey().getAddress().getHost() + ":" + memberEntry.getKey().getAddress().getPort();
                Map<Integer, Integer> result = memberEntry.getValue().get();
                result.entrySet().stream()
                .forEach(resultEntry -> collatedResults.put(resultEntry.getKey(), Tuple2.tuple2(resultEntry.getValue(), member)));
            } catch (Exception e) {
                LOGGER.error("logPartitions()", e);
            }
        });

        int partitionCountActual = collatedResults.size();
        int partitionCountExpected = this.hazelcastInstance.getPartitionService().getPartitions().size();

        // Less is ok, some may be empty.
        if (partitionCountActual > partitionCountExpected) {
            LOGGER.error("logPartitions() Results for {} partitions but expected {}",
                    partitionCountActual, partitionCountExpected);
            return;
        }

        double total = 0d;
        final AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);
        final AtomicInteger min = new AtomicInteger(Integer.MAX_VALUE);
        for (int i = 0 ; i < partitionCountExpected; i++) {
            if (collatedResults.containsKey(i)) {
                int count = collatedResults.get(i).f0();
                total += count;
                if (count > max.get()) {
                    max.set(count);
                }
                if (count < min.get()) {
                    min.set(count);
                }
            }
        }
        double average = total / partitionCountActual;
        double stdDev = this.calculateStdDev(collatedResults, average);

        LOGGER.info("-------------------------------------");
        collatedResults.entrySet()
        .stream()
        .forEach(entry -> {
            int count = entry.getValue().f0();
            LOGGER.info("Partition {} - entry count {} - member {} {}{}",
                    String.format("%3d", entry.getKey()),
                    String.format("%7d", count),
                    String.format("%22s", entry.getValue().f1()),
                    (count == max.get() ? "- MAXIMUM" : ""),
                    (count == min.get() ? "- MINIMUM" : "")
                    );
        });
        LOGGER.info("-------------------------------------");
        LOGGER.info("Total {}, StdDev {}, Maximum {}, Mininum {}",
                Double.valueOf(total).intValue(), stdDev, max, min);
        LOGGER.info("-------------------------------------");
    }


    /**
     * <p>Calculate the deviation from the average.
     * </p>
     *
     * @param collatedResults
     * @param average
     * @return
     */
    private double calculateStdDev(Map<Integer, Tuple2<Integer, String>> collatedResults, double average) {
        double total = collatedResults.values()
        .stream()
        .mapToDouble(value -> {
            double diff = value.f0() - average;
            return diff * diff;
        })
        .sum();

        return Math.sqrt(total / collatedResults.size());
    }

    /**
     * <p>What is running on a rebalance
     * </p>
     */
    private void logJobs() {
        List<Job> jobs = this.hazelcastInstance.getJet().getJobs();
        int count = jobs.size();
        Set<String> jobNames = jobs.stream().map(job -> job.getName()).collect(Collectors.toCollection(TreeSet::new));
        LOGGER.info("-------------------------------------");
        // Jobs with names in name order
        for (String jobName : jobNames) {
            this.logJobAndDeduct(jobName, jobs);
        }
        // Jobs without names
        for (Job job : jobs) {
            this.logJob(job);
        }
        LOGGER.info("-------------------------------------");
        LOGGER.info("{} job{}", count, (count == 1 ? "" : "s"));
        LOGGER.info("-------------------------------------");
    }

    /**
     * <p>Job info</p>
     */
    private void logJob(Job job) {
        if (job.getName().isBlank()) {
            LOGGER.info("Job {}, status {}", job.getId(),
                    String.format("%10s", job.getStatus().toString())
                    );
        } else {
            LOGGER.info("Job {}, status {}, name: {}", job.getId(),
                    String.format("%10s", job.getStatus().toString()),
                    job.getName());
        }
    }

    /**
     * <p>Remove job from list after printing. Job name is not unique,
     * this removes the first match it finds.
     * </p>
     * @param jobName
     * @param jobs
     */
    private void logJobAndDeduct(String jobName, List<Job> jobs) {
        int i;
        for (i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            if (!job.getName().isBlank() && job.getName().equals(jobName)) {
                this.logJob(job);
                jobs.remove(i);
                return;
            }
        }
        LOGGER.error("Jobname {} not found in {}", jobName, jobs);
    }
}
