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

package com.hazelcast.platform.demos.telco.churn.domain;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.platform.demos.telco.churn.MyConstants;

/**
 * <p>Derived sentiment for the customer's happiness, the higher the better.
 * </p>
 */
public class Sentiment implements Portable {

    private LocalDateTime updated = LocalDateTime.now();
    private double current;
    private double previous;

    @Override
    public int getClassId() {
        return MyConstants.CLASS_ID_SENTIMENT;
    }

    @Override
    public int getFactoryId() {
        return MyConstants.CLASS_ID_MYPORTABLEFACTORY;
    }

    /**
     * <p>Read fields, use defined order.</p>
     */
    @Override
    public void readPortable(PortableReader reader) throws IOException {
        long timestamp = reader.readLong("updated");
        this.updated = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault());
        this.current = reader.readDouble("current");
        this.previous = reader.readDouble("previous");
    }

    /**
     * <p>Write fields, use defined order.</p>
     */
    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        long timestamp = this.updated.atZone(ZoneId.systemDefault()).toEpochSecond();
        writer.writeLong("updated", timestamp);
        writer.writeDouble("current", this.current);
        writer.writeDouble("previous", this.previous);
    }

    // Generated code below

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getPrevious() {
        return previous;
    }

    public void setPrevious(double previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "Sentiment [updated=" + updated + ", current=" + current + ", previous=" + previous + "]";
    }

}
