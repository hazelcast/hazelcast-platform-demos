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

package com.hazelcast.platform.demos.retail.clickstream;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.hazelcast.map.MapLoader;
import com.hazelcast.map.MapStoreFactory;
import com.hazelcast.platform.demos.retail.clickstream.cassandra.ModelMapStore;
import com.hazelcast.platform.demos.retail.clickstream.cassandra.ModelRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Lazy lookup of repository beans from Spring.
 * </p>
 */
@SuppressWarnings("rawtypes")
@Component
@Slf4j
public class MyMapStoreFactory implements MapStoreFactory {

    @Autowired
    private ApplicationContext applicationContext;
    @Value("${spring.data.cassandra.contact-points}")
    private String contactPoints;

    @Override
    public MapLoader newMapStore(String arg0, Properties arg1) {
        if (arg0.equals(MyConstants.IMAP_NAME_MODEL_VAULT)) {
            ModelRepository modelRepository =
                    this.applicationContext.getBean(ModelRepository.class);
            return new ModelMapStore(modelRepository, contactPoints);
        }
        log.error("No map loader/store for map name '{}'", arg0);
        return null;
    }

}
