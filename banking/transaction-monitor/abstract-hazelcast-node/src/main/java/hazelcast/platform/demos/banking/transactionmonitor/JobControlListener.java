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

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

/**
 * <p>Listens on the "{@code job-control}" map for events in the form
 * <pre>
 * start IngestTransactions
 * stop AggregateQuery
 * </pre>
 * and acts accordingly.
 * </p>
 */
public class JobControlListener implements EntryAddedListener<String, String>, EntryUpdatedListener<String, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobControlListener.class);

    private transient Executor executor;
    private final String bootstrapServers;
    private final String pulsarList;
    private final boolean usePulsar;
    private final String projectName;
    private final String clusterName;
    private final TransactionMonitorFlavor transactionMonitorFlavor;

    public JobControlListener(String arg0, String arg1, boolean arg2, String arg3, String arg4,
            TransactionMonitorFlavor arg5) {
        this.executor = Executors.newSingleThreadExecutor();
        this.bootstrapServers = arg0;
        this.pulsarList = arg1;
        this.usePulsar = arg2;
        this.projectName = arg3;
        this.clusterName = arg4;
        this.transactionMonitorFlavor = arg5;
    }

    @Override
    public void entryUpdated(EntryEvent<String, String> event) {
        this.process(event);
    }

    @Override
    public void entryAdded(EntryEvent<String, String> event) {
        this.process(event);
    }

    /**
     * <p>Use runnables on local JVM to start, so events
     * consumed quickly.
     * </p>
     *
     * @param entryEvent
     */
    private void process(EntryEvent<String, String> entryEvent) {
        LOGGER.debug("Map '{}' : {} => ('{}','{}')",
                entryEvent.getSource(),
                entryEvent.getEventType(),
                entryEvent.getKey(), entryEvent.getValue());

        String verb = Objects.toString(entryEvent.getKey());
        String noun = Objects.toString(entryEvent.getValue());

        if (verb.toUpperCase(Locale.ROOT).equals("START")) {
            JobControlStartRunnable jobControlStartRunnable =
                    new JobControlStartRunnable(noun, this.bootstrapServers, this.pulsarList, this.usePulsar,
                            this.projectName, this.clusterName, this.transactionMonitorFlavor);
            this.executor.execute(jobControlStartRunnable);
        } else {
            if (verb.toUpperCase(Locale.ROOT).equals("STOP")) {
                JobControlStopRunnable jobControlStopRunnable = new JobControlStopRunnable(noun);
                this.executor.execute(jobControlStopRunnable);
            } else {
                LOGGER.error("Ignoring unknown verb in '{}' '{}'", verb, noun);
            }
        }
    }
}
