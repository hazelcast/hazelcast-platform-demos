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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>When a new model is out of validation, determine whether to use it.
 * </p>
 */
@Slf4j
public class RetrainingAssessmentListener implements EntryAddedListener<String, Double> {

    private transient Executor executor;

    public RetrainingAssessmentListener() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void entryAdded(EntryEvent<String, Double> arg0) {
        this.process(arg0);
    }

    private void process(EntryEvent<String, Double> entryEvent) {
        log.debug("Map '{}' : {} => ('{}','{}')",
                entryEvent.getSource(),
                entryEvent.getEventType(),
                entryEvent.getKey(), entryEvent.getValue());

        RetrainingAssessmentListenerRunnable retrainingAssessmentListenerRunnable
            = new RetrainingAssessmentListenerRunnable(entryEvent);
        log.trace("process(): execute {}", System.identityHashCode(retrainingAssessmentListenerRunnable));
        this.executor.execute(retrainingAssessmentListenerRunnable);
    }

}
