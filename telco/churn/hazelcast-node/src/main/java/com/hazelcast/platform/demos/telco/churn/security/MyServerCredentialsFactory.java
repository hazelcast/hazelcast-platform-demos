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

package com.hazelcast.platform.demos.telco.churn.security;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.security.Credentials;
import com.hazelcast.security.ICredentialsFactory;
import com.hazelcast.security.SimpleTokenCredentials;

/**
 * <p>A factory to create credentials for this server, to pass
 * to other servers when we wish to join a cluster.
 * </p>
 * <p>Returns the same credentials every time in this example,
 * token based.
 * </p>
 * <p><b>Note:</b> This is <em>simplified sample code</em>, do not use as-is production security.</p>
 */
public class MyServerCredentialsFactory implements ICredentialsFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyServerCredentialsFactory.class);

    private SimpleTokenCredentials myCredentials;
    private String timestamp;
    private String userName;

    /**
     * <p>Extract properties set in YAML file "{@code security-${my.environment}.yml}".
     * </p>
     */
    @Override
    public void init(Properties properties) {
        this.timestamp = properties.getProperty("timestamp");
        this.userName = properties.getProperty("userName");
    }

    @Override
    public void configure(CallbackHandler arg0) {
    }

    @Override
    public void destroy() {
    }

    /**
     * <p>Create CSV style token authentication credentials, once.
     * Member name is dynamically allocated and not available at this
     * point, so use IP address to distinguish server for logging.
     * </p>
     */
    @Override
    public Credentials newCredentials() {
        if (this.myCredentials == null) {
            String token = this.userName + "," + this.timestamp + ","
                    + this.getClass().getSimpleName() + ",";
            try {
                token += InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                LOGGER.error("newCredentials()", e);
                token += "?";
            }
            this.myCredentials = new SimpleTokenCredentials(token.getBytes(StandardCharsets.UTF_8));
        }
        return this.myCredentials;
    }

}
