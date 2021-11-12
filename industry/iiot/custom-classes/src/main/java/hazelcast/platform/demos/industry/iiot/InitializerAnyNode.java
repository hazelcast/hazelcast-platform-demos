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

package hazelcast.platform.demos.industry.iiot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.map.IMap;

/**
 * <p>Create necessary objects on demand rather than lazy, so as
 * visible on Management Center etc.
 * </p>
 */
public class InitializerAnyNode extends Tuple4Callable {
    private static final long serialVersionUID = 1L;
    private static final String MY_NAME = InitializerAnyNode.class.getSimpleName();

    private final String[][] config;

    InitializerAnyNode(String[][] arg0) {
        this.config = arg0;
    }

    @Override
    public List<Tuple4<String, String, List<String>, List<String>>> call() throws Exception {
        List<Tuple4<String, String, List<String>, List<String>>> result = new ArrayList<>();

        // Define mappings
        MappingDefinitions.addMappings(super.getHazelcastInstance())
        .stream().forEach(tuple4 -> result.add(tuple4));

        // Add config before touching maps that may have MapStores that use config
        this.populateConfig(result);
        // Define needed objects
        this.touchAllIMaps(result);

        return result;
    }

    /**
     * <p>Iterate through the entries to insert to the config map.
     * </p>
     *
     * @param result
     */
    private void populateConfig(List<Tuple4<String, String, List<String>, List<String>>> result) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String diagnostic = "";

        if (this.config == null) {
            errors.add("config is null");
        } else {
            if (this.config.length == 0) {
                errors.add("config is empty");
            } else {
                int count = 0;
                for (String[] pair : this.config) {
                    this.populateConfigEntry(pair, count, errors, warnings);
                    count++;
                }
                if (errors.size() == 0) {
                    diagnostic = "Inserted " + count + " item" + (count == 1 ? "" : "s");
                }
            }
        }

        result.add(Tuple4.tuple4(MY_NAME + ".populateConfig()", diagnostic, errors, warnings));
    }

    /**
     * <p>Insert an individual String/String pair into the config map.
     * </p>
     *
     * @param pair
     * @param count
     * @param errors
     * @param warnings
     */
    private void populateConfigEntry(String[] pair, int count, List<String> errors, List<String> warnings) {
        IMap<String, String> configMap = super.getHazelcastInstance().getMap(MyConstants.IMAP_NAME_SYS_CONFIG);
        if (pair == null) {
            errors.add("config pair " + count + " is null");
            return;
        }
        if (pair.length != 2
                || pair[0] == null || pair[0].length() == 0
                || pair[1] == null || pair[1].length() == 0) {
            errors.add("config pair " + count + " is invalid");
            for (int i = 0; i < pair.length; i++) {
                errors.add("config pair " + count + " item: " + i + " is: " + Objects.toString(pair[i]));
            }
            return;
        }
        // Overwrite is allowed, for idempotent testing
        Object previous = configMap.put(pair[0], pair[1]);
        if (previous != null) {
            warnings.add("Key '" + pair[0] + "' already present");
        }
    }

    /**
     * <p>Access maps to make them visible on Management Center.
     * </p>
     *
     * @param result
     */
    private void touchAllIMaps(List<Tuple4<String, String, List<String>, List<String>>> result) {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            super.getHazelcastInstance().getMap(iMapName);
            result.add(Tuple4.tuple4(MY_NAME + ".touchAllIMaps()", iMapName, new ArrayList<>(), new ArrayList<>()));
        }
    }

}
