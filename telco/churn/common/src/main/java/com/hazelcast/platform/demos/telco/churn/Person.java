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

package com.hazelcast.platform.demos.telco.churn;

import java.io.IOException;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

/**
 * XXX Temporary for SQL and GenericRecord.
 */
public class Person implements Portable {
    /**
     *
     */
    public static final int NINENINENINE = 999;
    private String firstName;
    private String lastName;
    @Override
    public String toString() {
        return "Person [firstName=" + firstName + ", lastName=" + lastName + "]";
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    @Override
    public int getClassId() {
        return NINENINENINE;
    }
    @Override
    public int getFactoryId() {
        return NINENINENINE;
    }
    @Override
    public void readPortable(PortableReader arg0) throws IOException {
        this.firstName = arg0.readUTF("firstName");
        this.lastName = arg0.readUTF("lastName");
    }
    @Override
    public void writePortable(PortableWriter arg0) throws IOException {
        arg0.writeUTF("firstName", this.firstName);
        arg0.writeUTF("lastName", this.lastName);
    }

}
