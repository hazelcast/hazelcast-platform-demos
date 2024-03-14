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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.core.Processor.Context;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.Sources;

/**
 * <p>A job to suspend/resume another job, {@link AlertLogger}
 * </p>
 */
public class AlertLoggerManager {

    private final Executor executor;
    private final HazelcastInstance hazelcastInstance;

    public AlertLoggerManager(Context context) {
        this.executor = Executors.newSingleThreadExecutor();
        this.hazelcastInstance = context.hazelcastInstance();
    }

    /**
     * <p>A simple job that stops/starts another job.
     * </p>
     * <pre>
     *                +------( 1 )------+
     *                | Journal Source  |
     *                +-----------------+
     *                         |
     *                         |
     *                         |
     *                +------( 2 )------+
     *                |  Custom Sink    |
     *                +-----------------+
     * </pre>
     * <p>
     * The steps:
     * </p>
     * <ol>
     * <li>
     * <p>
     * Map journal source
     * </p>
     * <p>Stream changes from the "{@code mongoActions}" map, only
     * the new value from writes.
     * </p>
     * </li>
     * <li>
     * <p>
     * Custom sink
     * </p>
     * <p>Suspend/resume another job.
     * </p>
     * </li>
     * </ol>
     *
     * @param clusterName
     * @return
     */
    public static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(Sources.mapJournal(MyConstants.IMAP_NAME_MONGO_ACTIONS,
                        JournalInitialPosition.START_FROM_OLDEST,
                        Util.mapEventNewValue(),
                        Util.mapPutEvents()
                    )
                ).withoutTimestamps()
        .writeTo(mySink());

        return pipeline;
    }

    /**
     * <p>Build sink to run single threaded.
     * </p>
     *
     * @return
     */
    public static Sink<Object> mySink() {
        return SinkBuilder.sinkBuilder(
                    "mySink-",
                    context -> new AlertLoggerManager(context)
                )
                .receiveFn(
                        (AlertLoggerManager mySink, Object item) -> mySink.receiveFn(item)
                        )
                .preferredLocalParallelism(1)
                .build();
    }

    /**
     * <p>Sink consumes the data and launches a thread to process,
     * to ensure no issues with deadlock on Hazelcast thread.
     * </p>
     *
     * @param object
     * @return
     */
    public Object receiveFn(Object object) {
        this.executor.execute(new AlertLoggerManagerRunnable(this.hazelcastInstance, object));
        return this;
    }
}
