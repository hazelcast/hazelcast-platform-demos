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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.cluster.Address;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.hazelcast.spi.partitiongroup.PartitionGroupStrategy;

/**
 * <p>Hazelcast provides WAN replication across Kubernetes installations.
 * see
 * <a href="https://github.com/hazelcast/hazelcast-code-samples/tree/master/hazelcast-integration/kubernetes/samples/wan">
 * here</a>. But if for some bizarre reason you wish to do it the hard way, this is the code to
 * do it yourself.
 * </p>
 * <p>This version uses DNS queries to Kubernetes itself to obtain the location
 * of each of the remote pods. For real use, this would be across two Kubenetes
 * systems, perhaps on different providers and with different rules. So this of it
 * more as a demo to base a solution upon. Real use will require security and
 * the Kubernetes API.
 * </p>
 * <p>When this is code invoked periodically, it is expected to return
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
    private static final Logger LOGGER = LoggerFactory.getLogger(MyWANDiscoveryStrategy.class);
    private static final String SRV_ATTRIBUTE_NAME = "SRV";
    private static final long FIVE_SECOND_TIMEOUT = 5000L;

    @Autowired
    private MyProperties myProperties;

    private DirContext dirContext;
    private TreeMap<String, DiscoveryNode> previousdiscoverNodes = new TreeMap<>();
    private String serviceDns;

    /**
     * <p>Hazelcast API method, called once to allow initialisation for the
     * local implementation.
     * </p>
     * <p>Create but don't yet use a {@link javax.naming.directory.DirContext}
     * for JNDI DNS lookup.
     * </p>
     */
    @Override
    public void start() {
        // We are looking for the other cluster
        this.serviceDns = this.myProperties.getRemoteSite().toString().toLowerCase(Locale.ROOT)
                + "-service.default.svc.cluster.local";

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");
        env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(FIVE_SECOND_TIMEOUT));

        try {
            this.dirContext = new InitialDirContext(env);
        } catch (Exception e) {
            LOGGER.error("start()", e);
        }
    }

    /**
     * <p>Hazelcast API method, called once to allow shutdown activities for
     * the implementation. Nothing needed.
     * </p>
     */
    @Override
    public void destroy() {
    }

    /**
     * <p>Hazelcast API method, called periodically to obtain a list of
     * host and ports for the pods in the remote cluster we wish to
     * connect to.
     * </p>
     */
    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        if (this.dirContext == null) {
            // Return if start() failed.
            return Collections.emptyList();
        }

        TreeMap<String, DiscoveryNode> currentDiscoverNodes = new TreeMap<>();

        // Actually do the discovery
        List<DiscoveryNode> nodes = this.discover();

        for (DiscoveryNode discoveryNode : nodes) {
            Address address = discoveryNode.getPrivateAddress();
            String addressStr = address.getHost() + ":" + address.getPort();
            currentDiscoverNodes.put(addressStr, discoveryNode);
        }

        if (LOGGER.isDebugEnabled()) {
            this.logDifference(this.previousdiscoverNodes, currentDiscoverNodes);
        }

        this.previousdiscoverNodes = currentDiscoverNodes;
        return currentDiscoverNodes.values();
    }

    /**
     * <p>Hazelcast API method, called once to find any local configuration
     * apart from properties. Nothing needed.
     * </p>
     */
    @Override
    public Map<String, String> discoverLocalMetadata() {
        return Collections.emptyMap();
    }

    /**
     * <p>Hazelcast API method, called once to if special partition placement
     * is needed so primary and backups are hosted apart. Nothing needed.
     * </p>
     */
    @Override
    public PartitionGroupStrategy getPartitionGroupStrategy() {
        return null;
    }

    /**
     * <p>Diagnostic logger for nodes joining and leaving.
     * </p>
     *
     * @param previous - set of discovery nodes
     * @param current - set of discovery nodes
     */
    private void logDifference(Map<String, DiscoveryNode> previous,
            Map<String, DiscoveryNode> current) {

        TreeSet<String> previousKeys = new TreeSet<>(previous.keySet());
        previousKeys.removeAll(current.keySet());
        for (String previousKey : previousKeys) {
            LOGGER.info("Node '{}' removed from discovery list", previousKey);
        }

        TreeSet<String> currentKeys = new TreeSet<>(current.keySet());
        currentKeys.removeAll(previous.keySet());
        for (String currentKey : currentKeys) {
            LOGGER.info("Node '{}' added to discovery list", currentKey);
        }
    }

    /**
     * <p>Query the DNS for the "SRV" server records matching
     * the service DNS. Site 1 looks up site 2, and vice versa.
     * </p>
     * @return
     */
    private List<DiscoveryNode> discover() {
        List<DiscoveryNode> result = new ArrayList<>();

        try {
            Set<String> hostAddresses = new HashSet<String>();

            Attributes attributes = dirContext.getAttributes(serviceDns,
                    new String[] { SRV_ATTRIBUTE_NAME });
            Attribute srvAttribute = attributes.get(SRV_ATTRIBUTE_NAME);

            NamingEnumeration<?> servers = srvAttribute.getAll();

            while (servers.hasMore()) {
                String server = servers.next().toString();

                String[] tokens = server.split(" ");

                // Hostname is last token, possibly ending full stop
                String hostName = tokens[tokens.length - 1];
                if (hostName.charAt(hostName.length() - 1) == '.') {
                    hostName = hostName.substring(0, hostName.length() - 1);
                }

                InetAddress inetAddress = InetAddress.getByName(hostName);

                hostAddresses.add(inetAddress.getHostAddress());
            }

            for (String hostAddress : hostAddresses) {
                result.add(new SimpleDiscoveryNode(new Address(hostAddress, NetworkConfig.DEFAULT_PORT)));
            }

        } catch (NameNotFoundException e) {
            // Remote site doesn't exist
            return result;
        } catch (Exception e) {
            LOGGER.error("discover()", e);
            return result;
        }

        return result;
    }

}
