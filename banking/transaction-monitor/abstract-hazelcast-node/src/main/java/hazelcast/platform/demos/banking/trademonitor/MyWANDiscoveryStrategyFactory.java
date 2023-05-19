/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.banking.trademonitor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

/**
 * <p>A factory to create {@link MyLocalWanDiscoveryStrategy} instances,
 * or in principal other types of discovery strategies for WAN.
 * In fact always return a singleton.
 * </p>
 */
public class MyWANDiscoveryStrategyFactory implements DiscoveryStrategyFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyWANDiscoveryStrategyFactory.class);

    private MyLocalWANDiscoveryStrategy myLocalDiscoveryStrategy;

    MyWANDiscoveryStrategyFactory() {
    }
    MyWANDiscoveryStrategyFactory(String localClusterName, Properties properties) {
        this.myLocalDiscoveryStrategy = new MyLocalWANDiscoveryStrategy(localClusterName, properties);
    }

    /**
     * <p>Not currently configurable.
     * </p>
     */
    @Override
    public Collection<PropertyDefinition> getConfigurationProperties() {
        return Collections.emptyList();
    }

    /**
     * <p>Type of the returned object.
     * </p>
     */
    @Override
    public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
        return MyLocalWANDiscoveryStrategy.class;
    }

    /**
     * <p>Return the singleton rather than construct on demand.
     * </p>
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode, ILogger logger,
            Map<String, Comparable> properties) {
        if (this.myLocalDiscoveryStrategy == null) {
            LOGGER.error("this.myLocalDiscoveryStrategy == null");
        }
        return this.myLocalDiscoveryStrategy;
    }

}
