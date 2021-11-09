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

import com.hazelcast.jet.datamodel.Tuple4;

/**
 * <p>Create necessary objects on demand rather than lazy, so as
 * visible on Management Center etc.
 * </p>
 */
public class InitializerAnyNode extends Tuple4Callable {
    private static final long serialVersionUID = 1L;
    private static final String MY_NAME = InitializerAnyNode.class.getSimpleName();

    @Override
    public List<Tuple4<String, String, List<String>, List<String>>> call() throws Exception {
        List<Tuple4<String, String, List<String>, List<String>>> result = new ArrayList<>();

        // Define mappings
        MappingDefinitions.addMappings(super.getHazelcastInstance())
        .stream().forEach(tuple4 -> result.add(tuple4));

        // Define needed objects
        this.touchIMaps(result);

        return result;
    }

    private void touchIMaps(List<Tuple4<String, String, List<String>, List<String>>> result) {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            super.getHazelcastInstance().getMap(iMapName);
            result.add(Tuple4.tuple4(MY_NAME + ".touchIMaps()", iMapName, new ArrayList<>(), new ArrayList<>()));
        }
    }

}
