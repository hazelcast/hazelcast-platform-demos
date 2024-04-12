/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.utils;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.version.Version;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>
 * Idempotent callable, used to help confirm clusterside classes have been uploaded
 * and version compatible with caller for future operations.
 * </p>
 */
public class ConnectIdempotentCallable implements Callable<Tuple2<Version, List<String>>>, HazelcastInstanceAware, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String FILENAME = ConnectIdempotentCallable.class.getSimpleName() + ".properties";

    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Set by setHazelcastInstance()")
    private transient HazelcastInstance hazelcastInstance;

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception can be thrown by properties.load()")
    @Override
    public Tuple2<Version, List<String>> call() throws Exception {
        String clusterName = this.hazelcastInstance.getConfig().getClusterName();

        Version v = this.hazelcastInstance.getCluster().getClusterVersion();
        List<String> l = new ArrayList<>();

        ClassLoader classLoader = this.getClass().getClassLoader();

        Properties properties = new Properties();
        try (InputStream inputStream = classLoader.getResourceAsStream(FILENAME)) {
            properties.load(inputStream);
            if (properties.isEmpty()) {
                l.add("On " + clusterName + " '" + FILENAME + "' is empty");
            }
            for (Entry<?, ?> entry : properties.entrySet()) {
                String message = String.format("On %s == %s:'%s'=='%s'",
                        clusterName, FILENAME, entry.getKey().toString(), entry.getValue().toString());
                l.add(message);
            }
        } catch (Exception e) {
            l.add("'" + FILENAME + "' exception:" + e.getMessage());
        }

        return Tuple2.tuple2(v, l);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Safe to share mutable")
    @Override
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
