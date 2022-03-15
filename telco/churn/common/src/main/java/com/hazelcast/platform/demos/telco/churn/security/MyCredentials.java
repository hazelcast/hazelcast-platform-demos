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

package com.hazelcast.platform.demos.telco.churn.security;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.platform.demos.telco.churn.MyConstants;
import com.hazelcast.security.Credentials;

/**
 * <p>This is not currently used. It is present to demonstrate how credentials could
 * be provided from non-Java clients.
 * </p>
 */
public class MyCredentials implements Credentials, IdentifiedDataSerializable {
    private static final long serialVersionUID = 1L;

    private String token;

    // Required to implement {@link com.hazelcast.security.Credentials}
    @Override
    public String getName() {
        return this.token;
    }

    // Required to implement {@link com.hazelcast.nio.serialization.IdentifiedDataSerializable}
    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(this.token);
    }
    @Override
    public void readData(ObjectDataInput in) throws IOException {
        this.token = in.readUTF();
    }
    @Override
    public int getFactoryId() {
        return MyConstants.CLASS_ID_MYDATASERIALIZABLEFACTORY;
    }
    @Override
    public int getClassId() {
        return MyConstants.CLASS_ID_MYCREDENTIALS;
    }

    // Standard getters, setters &amp; toString
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    @Override
    public String toString() {
        return "MyCredentials [token=" + token + "]";
    }

}
