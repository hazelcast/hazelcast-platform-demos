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

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.partitiongroup.PartitionGroupStrategy;

/**
 * <p>How "{@code site1}" finds "{@code site2}" and vice
 * versa.
 * </p>
 * <p>When this is invoked periodically, it is expected to return
 * a list of node &amp; port pairs for the remote cluster.
 * </p>
 * <p>In essence, the code is:
 * <pre>
 *   Address privateAddress = new Address("127.0.0.1", 5701);
 *   DiscoveryNode discoveryNode = new SimpleDiscoveryNode(privateAddress);
 *   nodes.add(discoveryNode);
 * </pre>
 * with the detail being to find the IP address and port for each
 * node currently in the remote cluster.
 * </p>
 */
@Component
public class MyWANDiscoveryStrategy implements DiscoveryStrategy {

    //TODO Add an implementation
    /**
     * <p>This method finds the nodes of the other cluster as
     * host &amp; port pairs, and turns them into a list.
     * </p>
     */
    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        return Collections.emptyList();
    }

    /**
     * <p>No special implementation required.
     * </p>
     */
    @Override
    public void destroy() {
    }

    /**
     * <p>No special implementation required.
     * </p>
     */
    @Override
    public Map<String, String> discoverLocalMetadata() {
        return Collections.emptyMap();
    }

    /**
     * <p>No special implementation required.
     * </p>
     */
    @Override
    public PartitionGroupStrategy getPartitionGroupStrategy() {
        return null;
    }

    /**
     * <p>No special implementation required.
     * </p>
     */
    @Override
    public void start() {
    }

}
