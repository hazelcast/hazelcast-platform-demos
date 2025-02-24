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

package hazelcast.platform.demos.benchmark.nexmark.model;

/**
 * <p>Base for auction events types.
 * </p>
 */
public abstract class Event {
    private final long id;
    private final long timestamp;

    public Event(long id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public long id() {
        return id;
    }

    public long timestamp() {
        return timestamp;
    }
}
