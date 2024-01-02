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

package hazelcast.platform.demos.banking.transactionmonitor;

import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Use a CP {@link com.hazelcast.cp.IAtomicLong IAtomicLong} as a sink.
 * </p>
 */
public class CPAtomicLongSink {
    private final CPSubsystem cpSubsystem;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Object is thread-safe")
    public CPAtomicLongSink(CPSubsystem cpSubsystem) {
        this.cpSubsystem = cpSubsystem;
    }

    /**
     * <p>Build the sink, mainly {@link #receiveFn(String)}.
     * </p>
     *
     * @param accessToken For access to Slack
     * @return
     */
    public static Sink<String> cpAtomicLongSink() {
        return SinkBuilder.sinkBuilder(
                    "atomicLongSink-",
                    context -> new CPAtomicLongSink(context.hazelcastInstance().getCPSubsystem())
                )
                .receiveFn(
                        (CPAtomicLongSink cpAtomicLongSink, String key) -> cpAtomicLongSink.receiveFn(key)
                        )
                .build();
    }

    /**
     * <p>Increment the counter for the key.
     * </p>
     *
     * @param keu
     * @return
     */
    public Object receiveFn(String key) {
        int groupSelector = Math.abs(key.hashCode() % 2);

        String group = (groupSelector == 1) ? MyConstants.CP_GROUP_A : MyConstants.CP_GROUP_B;

        this.cpSubsystem.getAtomicLong(key + "@ " + group).incrementAndGet();

        return this;
    }

}
