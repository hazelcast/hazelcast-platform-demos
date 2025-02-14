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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Map.Entry;

import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Use a CP {@link com.hazelcast.cp.CPMap CPMap} as a sink.
 * </p>
 */
public class CPMapSink {
    private final CPSubsystem cpSubsystem;
    private final String cpMapName;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Object is thread-safe")
    public CPMapSink(CPSubsystem cpSubsystem, String cpMapName) {
        this.cpSubsystem = cpSubsystem;
        this.cpMapName = cpMapName;
    }

    /**
     * <p>Build the sink, mainly {@link #receiveFn(Entry<String, Tuple3<Long, Double, Double>>)}.
     * </p>
     *
     * @return
     */
    public static Sink<Entry<String, Tuple3<Long, Double, Double>>> cpMapSink(String cpMapName) {
        return SinkBuilder.sinkBuilder(
                    "cpMapSink-",
                    context -> new CPMapSink(context.hazelcastInstance().getCPSubsystem(), cpMapName)
                )
                .receiveFn(
                        (CPMapSink cpMapSink, Entry<String, Tuple3<Long, Double, Double>> entry) -> cpMapSink.receiveFn(entry)
                        )
                .build();
    }

    /**
     * <p>Replace the entry in the CP Map.
     * </p>
     *
     * @param key/value pair
     * @return
     */
    public Object receiveFn(Entry<String, Tuple3<Long, Double, Double>> entry) {
        this.cpSubsystem.getMap(this.cpMapName).set(entry.getKey(), entry.getValue());

        return this;
    }

}
