/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.telco.churn.mapstore;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.MapInterceptor;

/**
 * <p>This doesn't fire for a {@link MapLoader} or {@link EntryProcessor} as
 * these have their own server-side logic so shouldn't need extra.</p>
 * <p>It does for for Jet jobs with a map sink and for the more basic
 * "{@code Map.put(K,V))}" operations.
 * </p>
 */
public class UpdatedByMapInterceptor implements MapInterceptor {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatedByMapInterceptor.class);

    private String modifier;

    public UpdatedByMapInterceptor(String arg0) {
        this.modifier = arg0;
    }

    /**
     * <p>Pre-call intercept, ensure "{@code lastModifiedBy}" and
     * "{@code lastModifiedDate}" are set.
     * </p>
     */
    @Override
    public Object interceptPut(Object oldValue, Object newValue) {
        LOGGER.trace("interceptPut({}, {})", oldValue, newValue);
        if (newValue instanceof HazelcastJsonValue) {
            try {
                JSONObject json = new JSONObject(newValue.toString());
                long now = System.currentTimeMillis();

                json.put("lastModifiedBy", this.modifier);
                json.put("lastModifiedDate", now);

                return new HazelcastJsonValue(json.toString());
            } catch (Exception exception) {
                LOGGER.error("interceptPut('{}'), EXCEPTION: {}", newValue, exception.getMessage());
            }
        }
        return newValue;
    }

    /**
     * <p>Pre-call intercept, no-op</p>
     */
    @Override
    public Object interceptGet(Object value) {
        LOGGER.trace("interceptGet({})", value);
        return value;
    }
    /**
     * <p>Pre-call intercept, no-op</p>
     */
    @Override
    public Object interceptRemove(Object removedValue) {
       LOGGER.trace("interceptRemove({})", removedValue);
       return removedValue;
    }

    /**
     * <p>Post-call intercept, no-op</p>
     */
    @Override
    public void afterGet(Object value) {
    }
    /**
     * <p>Post-call intercept, no-op</p>
     */
    @Override
    public void afterPut(Object value) {
    }
    /**
     * <p>Post-call intercept, no-op</p>
     */
    @Override
    public void afterRemove(Object oldValue) {
    }

    // Generated code below

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modifier == null) ? 0 : modifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UpdatedByMapInterceptor other = (UpdatedByMapInterceptor) obj;
        if (modifier == null) {
            if (other.modifier != null) {
                return false;
            }
        } else if (!modifier.equals(other.modifier)) {
            return false;
        }
        return true;
    }

}
