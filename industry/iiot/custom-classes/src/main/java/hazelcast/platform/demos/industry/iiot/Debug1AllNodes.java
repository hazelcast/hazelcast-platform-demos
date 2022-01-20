/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Idempotent debug callable, to prove remote execution works.
 * </p>
 */
public class Debug1AllNodes implements Callable<List<String>>, HazelcastInstanceAware, Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
            justification = "setHazelcastInstance() gets called by Hazelcast")
    private transient HazelcastInstance hazelcastInstance;

    /**
     * <p>Return basic information.
     * </p>
     */
    @Override
    public List<String> call() throws Exception {
        List<String> result = new ArrayList<>();

        long now = System.currentTimeMillis();
        String message = String.format("%s System.currentTimeMillis()==%d (%s)",
                this.hazelcastInstance.getName(), now, new Date(now).toString());
        result.add(message);

        return result;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    @Override
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
