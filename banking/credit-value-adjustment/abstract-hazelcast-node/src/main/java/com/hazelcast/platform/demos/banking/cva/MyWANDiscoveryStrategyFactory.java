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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

/**
 * <p>A factory to create {@link MyLocalWantDiscoveryStrategy} instances,
 * or in principal other types of discovery strategies for WAN.
 * In fact always return a singleton Spring {@code @Bean}.
 * </p>
 */
@Component
public class MyWANDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

    @Autowired
    private MyLocalWANDiscoveryStrategy myLocalDiscoveryStrategy;

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
     * <p>Return the autowired Spring bean rather than construct on demand.
     * </p>
     */
    @SuppressWarnings("rawtypes")
    @Override
    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode, ILogger logger,
            Map<String, Comparable> properties) {
        return this.myLocalDiscoveryStrategy;
    }

}
