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

package com.hazelcast.platform.demos.banking.cva;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.jet.datamodel.Tuple2;

/**
 * <p>
 * After data is loaded, confirm how it is spread.
 * </p>
 */
@Component
@Order(value = 4)
public class DataValidation implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataValidation.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * <p>
     * Call a {@code Callable} running on the grid to determine mapping of
     * partitions to members.
     * </p>
     */
    @Override
    public void run(String... args) throws Exception {
        IExecutorService iExecutorService = this.hazelcastInstance.getExecutorService("default");

        String[] mapNames = new String[] { MyConstants.IMAP_NAME_IRCURVES, MyConstants.IMAP_NAME_TRADES };

        for (String mapName : mapNames) {
            LOGGER.info("=== '{}' ===", mapName);

            MapMemberPartitionCallable mapMemberPartitionCallable = new MapMemberPartitionCallable(mapName);

            SortedMap<String, SortedMap<Integer, Long>> prettyPrint = new TreeMap<>();

            Map<Member, Future<Tuple2<String, Map<Integer, Long>>>> results = iExecutorService
                    .submitToAllMembers(mapMemberPartitionCallable);

            for (Map.Entry<Member, Future<Tuple2<String, Map<Integer, Long>>>> entry : results.entrySet()) {
                Tuple2<String, Map<Integer, Long>> result = entry.getValue().get();
                prettyPrint.put(result.f0(), new TreeMap<>(result.f1()));
            }

            LOGGER.info("@@@ #Member,Partition,Count");
            for (Map.Entry<String, SortedMap<Integer, Long>> entry : prettyPrint.entrySet()) {
                for (Map.Entry<Integer, Long> subEntry : entry.getValue().entrySet()) {
                    LOGGER.info("@@@ {},{},{}", entry.getKey(), subEntry.getKey(), subEntry.getValue());
                }
            }

            LOGGER.info("=== '{}' ===", mapName);
        }
    }
}
