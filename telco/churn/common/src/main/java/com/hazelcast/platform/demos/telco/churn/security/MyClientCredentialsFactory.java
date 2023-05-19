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

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.TimeZone;

import javax.security.auth.callback.CallbackHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.security.Credentials;
import com.hazelcast.security.ICredentialsFactory;
import com.hazelcast.security.SimpleTokenCredentials;

/**
 * <p>A factory to create credentials for a client, to pass
 * to the cluster when attempting to connect.
 * </p>
 * <p>Returns the same credentials every time in this example,
 * token based.
 * </p>
 * <p><b>Note:</b> This is <em>simplified sample code</em>, do not use as-is for Production security.</p>
 */
public class MyClientCredentialsFactory implements ICredentialsFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyClientCredentialsFactory.class);

    private Credentials myCredentials;
    private String buildTimestamp;
    private String moduleName;
    private String userName;

    /**
     * <p>Extract properties set in YAML file "{@code security-${my.environment}.yml}".
     * </p>
     */
    @Override
    public void init(Properties properties) {
        this.buildTimestamp = properties.getProperty("buildTimestamp");
        this.moduleName = properties.getProperty("moduleName");
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
     * </p>
     */
    @Override
    public Credentials newCredentials() {
        if (this.myCredentials == null) {
            // Due to NATting this may not match receiver's view of sender's endpoint
            String ip = "?";
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                LOGGER.error("newCredentials()", e);
            }

            // Run start timestamp, to counterpart build timestamp. In Docker so use universal time
            long now = System.currentTimeMillis();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String startTimestamp = simpleDateFormat.format(new Date(now));

            String token = this.userName
                    + "," + this.buildTimestamp
                    + "," + this.getClass().getSimpleName()
                    + "," + this.moduleName
                    + "," + startTimestamp
                    + "," + ip;

            LOGGER.info("newCredentials => {}", token);
            this.myCredentials = new SimpleTokenCredentials(token.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("newCredentials => '{}'", this.myCredentials.getName());
        }
        return this.myCredentials;
    }

}
