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
import java.util.Arrays;
import java.util.Map;

import javax.naming.Context;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.security.ClusterIdentityPrincipal;
import com.hazelcast.security.Credentials;
import com.hazelcast.security.CredentialsCallback;
import com.hazelcast.security.EndpointCallback;
import com.hazelcast.security.SimpleTokenCredentials;

/**
 * <p>This validates incoming credentials coming from another
 * <u>server</u> trying to join the cluster. Return "{@code true}"
 * to allow and "{@code false}" for reject, unsurprisingly.
 * </p>
 * <p>JAAS allows multiple login modules to be used, and the first
 * to fail stops the login. Here there is only one.
 * </p>
 * <p>Servers use {@link com.hazelcast.security.SimpleTokenCredentials SimpleTokenCredentials}.
 * </p>
 * <p><b>Note:</b> This is <em>simplified sample code</em>, do not use as-is for Production security.</p>
 */
public class MyServerLoginModule implements LoginModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyServerLoginModule.class);
    private static final int QUADRUPLE = 4;

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;
    private String myBuildTimestamp;

    /**
     * <p>Prepare the login module for this connection attempt, capture
     * the state.
     * </p>
     *
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options Properties set in "{@code security-${my.environment}.yml}".
     */
    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        String myIp = "?";
        try {
            myIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            LOGGER.error("initialize", e);
        }

        LOGGER.trace("initialize({}, {}, {}, {}), my IP={}",
                subject.getPrincipals(),
                callbackHandler.getClass().getSimpleName(),
                sharedState.keySet(),
                options.keySet(),
                myIp);
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = (Map<String, Object>) sharedState;
        this.myBuildTimestamp = options.get("buildTimestamp").toString();
    }

    /**
     * <p>Accept or reject the login. We expect a 4-tuple of
     * build name, build timestamp, the word "{@code server}", and the
     * IP address of the sender.
     * </p>
     * <p> The next call will be a {@link #commit} or {@link #abort},
     * based on all "{@code true}" or any "{@ codefalse}" in the login chain.
     * </p>
     */
    @Override
    public boolean login() throws LoginException {
        CredentialsCallback credentialsCallback = new CredentialsCallback();
        EndpointCallback endpointCallback = new EndpointCallback();

        try {
            this.callbackHandler.handle(new Callback[]{credentialsCallback, endpointCallback});
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }

        Credentials credentials = credentialsCallback.getCredentials();
        String endpoint = endpointCallback.getEndpoint();

        if (credentials == null) {
            LOGGER.error("login(), credentials are null");
            return false;
        }
        if (!(credentials instanceof SimpleTokenCredentials)) {
            LOGGER.error("login(), credentials are not SimpleTokenCredentials");
            return false;
        }
        if (endpoint == null) {
            LOGGER.error("login(), endpoint is null");
            return false;
        }

        SimpleTokenCredentials simpleTokenCredentials = (SimpleTokenCredentials) credentials;
        String[] tokens = new String(simpleTokenCredentials.getToken(), StandardCharsets.UTF_8).split(",");
        if (tokens.length < QUADRUPLE) {
            LOGGER.error("login() tokens=='{}'", Arrays.asList(tokens));
            return false;
        }

        String theirBuildTimestamp = tokens[1];
        String kind = tokens[2];
        String module = tokens[3];

        if (!MyServerCredentialsFactory.class.getSimpleName().equals(kind)) {
            LOGGER.error("login() wrong kind for '{}'", Arrays.asList(tokens));
            return false;
        }

        // Tolerable for clients but not for servers
        if (!this.myBuildTimestamp.equals(theirBuildTimestamp)) {
            LOGGER.error("login() my build '{}' different from their build in '{}'",
                this.myBuildTimestamp, Arrays.asList(tokens));
            return false;
        }

        LOGGER.info("login() endpoint='{}', tokens=='{}'", endpoint, Arrays.asList(tokens));
        this.sharedState.put(Context.SECURITY_CREDENTIALS, module);
        return true;
    }

    /**
     * <p>If all login modules used agree, commit the login.
     * </p>
     * <p>Retain the security principle, more as best practice.
     * We currently don't use it again.
     * </p>
     */
    @Override
    public boolean commit() throws LoginException {
        String name = (String) sharedState.get(Context.SECURITY_CREDENTIALS);

        ClusterIdentityPrincipal clusterIdentityPrincipal = new ClusterIdentityPrincipal(name);

        this.subject.getPrincipals().add(clusterIdentityPrincipal);

        this.sharedState.put(Context.SECURITY_PRINCIPAL, clusterIdentityPrincipal);

        LOGGER.trace("commit() principal=='{}'", clusterIdentityPrincipal);
        return true;
    }

    /**
     * <p>Use {@link #logout} to abort the login.
     * </p>
     */
    @Override
    public boolean abort() throws LoginException {
        LOGGER.info("abort()");
        return this.logout();
    }

    /**
     * <p>The opposite of {@link #login}, clear any saved tokens to effect
     * the logout.
     * </p>
     */
    @Override
    public boolean logout() throws LoginException {
        LOGGER.info("logout({})", this.subject.getPrincipals());
        this.sharedState.clear();
        this.subject.getPrincipals().clear();
        return true;
    }
}
