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

package com.hazelcast.platform.demos.retail.clickstream;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryExpiredListener;
import com.hazelcast.map.listener.EntryLoadedListener;
import com.hazelcast.map.listener.EntryMergedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>An {@link IMap} listener logging all event types.
 * </p>
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class MyLoggingListener implements EntryAddedListener,
    EntryEvictedListener, EntryExpiredListener,
    EntryLoadedListener, EntryMergedListener,
    EntryRemovedListener, EntryUpdatedListener {

    @Override
    public void entryAdded(EntryEvent entryEvent) {
        this.log(entryEvent);
    }
    @Override
    public void entryEvicted(EntryEvent entryEvent) {
        this.log(entryEvent);
    }
    @Override
    public void entryExpired(EntryEvent entryEvent) {
        this.log(entryEvent);
    }
    @Override
    public void entryLoaded(EntryEvent entryEvent) {
        this.log(entryEvent);
    }
    @Override
    public void entryMerged(EntryEvent entryEvent) {
        this.log(entryEvent);
    }
    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        this.log(entryEvent);
    }
    @Override
    public void entryRemoved(EntryEvent entryEvent) {
        this.log(entryEvent);
    }

    private void log(EntryEvent entryEvent) {
        log.info("Map '{}' : {} => ('{}','{}')",
                entryEvent.getSource(),
                entryEvent.getEventType(),
                entryEvent.getKey(), entryEvent.getValue());
    }
}
