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

package com.hazelcast.platform.demos.telco.churn.mapstore;

import java.util.Properties;

import com.hazelcast.map.MapLoader;
import com.hazelcast.map.MapStoreFactory;
import com.hazelcast.platform.demos.telco.churn.MyConstants;
import com.hazelcast.platform.demos.telco.churn.MyProperties;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordRepository;
import com.hazelcast.platform.demos.telco.churn.domain.CustomerRepository;
import com.hazelcast.platform.demos.telco.churn.domain.TariffRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * <p>Create {@link MapLoader} for specific maps.
 * </p>
 * <p>A {@link MapLoader} imports data, defined for Cassandra, Mongo and MySql.
 * Cassandra is also a {@link MapStore} so can export data back to Cassandra
 * if it is changed in the grid.
 * </p>
 */
@SuppressWarnings("rawtypes")
@Component
public class MyMapStoreFactory implements MapStoreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyMapStoreFactory.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private MyProperties myProperties;

    /**
     * <p>Return a {@link MapLoader} (legacy into Hazelcast), which may
     * also be a {@link MapStore} (legacy into Hazelcast, Hazelcast into legacy)
     * </p>
     */
    @Override
    public MapLoader newMapStore(String mapName, Properties properties) {
        LOGGER.trace("newMapStore({}, {})", mapName, properties);

        MapLoader mapLoader = null;

        switch (mapName) {
        case MyConstants.IMAP_NAME_CDR:
            CallDataRecordRepository callDataRecordRepository =
                this.applicationContext.getBean(CallDataRecordRepository.class);
            mapLoader = new CallDataRecordMapStore(callDataRecordRepository,
                    MyMapHelpers.getModifiedBy(this.myProperties));
            break;
        case MyConstants.IMAP_NAME_CUSTOMER:
            CustomerRepository customerRepository =
                this.applicationContext.getBean(CustomerRepository.class);
            mapLoader = new CustomerMapStore(customerRepository,
                    MyMapHelpers.getModifiedBy(this.myProperties));
            break;
        case MyConstants.IMAP_NAME_TARIFF:
            TariffRepository tariffRepository =
                this.applicationContext.getBean(TariffRepository.class);
            mapLoader = new TariffMapLoader(tariffRepository);
            break;
        default:
            break;
        }

        if (mapLoader == null) {
            throw new RuntimeException("No @Bean for mapName==" + mapName);
        }

        return mapLoader;
    }

}
