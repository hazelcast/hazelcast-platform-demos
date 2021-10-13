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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.util.Arrays;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.map.IMap;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Find the digital twin information from the corresponding
 * map, and format to CSV style.
 * </p>
 * <p>Expected output:
 * <pre>
 *  "data,aaaa,123,456,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1"
 * </pre>
 * </p>
 * <p>Prefix "data", then key, publish time, ingest time, and observed actions.
 * </p>
 */
@Slf4j
public class DigitalTwinProcessor extends AbstractProcessor {
    private final IMap<String, Tuple3<Long, Long, String>> digitalTwinMap;

    public DigitalTwinProcessor(HazelcastInstance hazelcastInstance) {
        this.digitalTwinMap = hazelcastInstance.getMap(MyConstants.IMAP_NAME_DIGITAL_TWIN);
    }

    /**
     * <p>Occasionally the input key, which comes from the "{@code checkout}"
     * map will be available before the corresponding entry has been written
     * to the "{@code digital_twin}" map, especially if checkout is the
     * only action. Log this but ignore it.
     * </p>
     */
    @Override
    protected boolean tryProcess(int ordinal, Object item) {
        String key = item.toString();
        Tuple3<Long, Long, String> value = this.digitalTwinMap.get(key);
        if (value == null) {
            log.trace("tryProcess({}, '{}') -> null", ordinal, item);
            return true;
        }
        String arrPrint =
                Arrays.toString(MyUtils.digitalTwinCsvToBinary(null, value.f2(), false));
        String result = "data," + key + "," + value.f0() + "," + value.f1() + "," + arrPrint.substring(1, arrPrint.length() - 1);
        return super.tryEmit(result);
    }

}
