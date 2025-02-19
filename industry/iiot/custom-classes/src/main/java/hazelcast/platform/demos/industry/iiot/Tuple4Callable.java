/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.jet.datamodel.Tuple4;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Base class for callables sent to the grid that return a list of
 * three things:
 * <ol>
 * <li><p><b>f0</b> Main info.
 * </p></li>
 * <li><p><b>f1</b> Optional additional info.
 * </p></li>
 * <li><p><b>f2</b> A list of any errors associated. No errors means it worked.
 * </p></li>
 * <li><p><b>f3</b> A list of any warnings associated. Warnings still means it worked.
 * </p></li>
 * </ol>
 * </p>
 */
public abstract class Tuple4Callable implements Callable<List<Tuple4<String, String, List<String>, List<String>>>>,
    HazelcastInstanceAware, Serializable {
    private static final long serialVersionUID = 1L;

    private transient HazelcastInstance hazelcastInstance;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    @Override
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Hazelcast instance must be shared, not cloned")
    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

}
