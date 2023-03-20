/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.security.Credentials;
import com.hazelcast.security.ICredentialsFactory;
import com.hazelcast.security.UsernamePasswordCredentials;

/**
 * <p>A factory to create username/password credentials for the
 * Management Center to authenticate itself to the Hazelcast cluster.
 * This can be done from the config file, "{@code hazelcast-client-cluster1.xml}"
 * but do it from code as a demonstration.
 * </p>
 * <p><b>Note:</b> This is <em>simplified sample code</em>, do not use as-is for Production security.</p>
 */
public class MyMCCredentialsFactory implements ICredentialsFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyMCCredentialsFactory.class);

    private Credentials myCredentials;

    @Override
    public void init(Properties properties) {
    }

    @Override
    public void configure(CallbackHandler arg0) {
    }

    @Override
    public void destroy() {
    }

    /**
     * <p>Create logon/password style credentials. For real usage, these
     * would not be hardcoded,
     * </p>
     */
    @Override
    public Credentials newCredentials() {
        if (this.myCredentials == null) {
            String username = "ManagementCenter";
            String password = "znantrzragpragre";
            this.myCredentials = new UsernamePasswordCredentials(username, password);
            LOGGER.info("newCredentials => {}", this.myCredentials);
        }
        return this.myCredentials;
    }

}
