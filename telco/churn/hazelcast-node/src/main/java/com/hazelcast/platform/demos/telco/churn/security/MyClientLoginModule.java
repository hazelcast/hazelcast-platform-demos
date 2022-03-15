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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.naming.Context;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.platform.demos.telco.churn.MyUtils;
import com.hazelcast.security.ClusterIdentityPrincipal;
import com.hazelcast.security.ClusterRolePrincipal;
import com.hazelcast.security.Credentials;
import com.hazelcast.security.CredentialsCallback;
import com.hazelcast.security.EndpointCallback;
import com.hazelcast.security.SimpleTokenCredentials;
import com.hazelcast.security.UsernamePasswordCredentials;

/**
 * <p>This validates incoming credentials coming from a
 * <u>client</u> trying to join the cluster. Return "{@code true}"
 * to allow and "{@code false}" for reject, unsurprisingly.
 * </p>
 * <p>JAAS allows multiple login modules to be used, and the first
 * to fail stops the login. Here there is only one.
 * </p>
 * <p>Clients may use {@link com.hazelcast.security.SimpleTokenCredentials SimpleTokenCredentials}
 * or {@link com.hazelcast.security.UsernamePasswordCredentials UsernamePasswordCredentials} depending
 * on their configuration, so support both types.
 * </p>
 * <p><b>Note:</b> This is <em>simplified sample code</em>, do not use as-is for Production security.</p>
 */
public class MyClientLoginModule implements LoginModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyClientLoginModule.class);
    private static final int QUADRUPLE = 4;
    private static boolean loggedOnce;

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;
    private String myBuildTimestamp;
    private List<String> blockedPasswords;

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
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "sharedState is shared")
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
        this.blockedPasswords = new ArrayList<>();
        String blockedPasswordsCsv = options.get("blockedPasswordsCsv").toString();
        for (String blockedPassword : blockedPasswordsCsv.split(",")) {
            this.blockedPasswords.add(blockedPassword);
            if (passwordLoggingOn()) {
                LOGGER.debug("Adding '{}' to blocked password list", blockedPassword);
            }
        }
        setPasswordLoggingOff();
    }

    /**
     * <p>Accept or reject the login. Allow it to be a token or a username/password
     * and validate accordingly.
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
        String apparentEndpoint = endpointCallback.getEndpoint();

        if (credentials == null) {
            LOGGER.error("login(), credentials are null");
            return false;
        }
        if (apparentEndpoint == null) {
            LOGGER.error("login(), apparentEndpoint is null");
            return false;
        }

        if (credentials instanceof SimpleTokenCredentials) {
            return loginSimpleTokenCredentials((SimpleTokenCredentials) credentials, apparentEndpoint);
        }
        if (credentials instanceof UsernamePasswordCredentials) {
            return loginUsernamePasswordCredentials((UsernamePasswordCredentials) credentials, apparentEndpoint);
        }

        LOGGER.error("login(), credentials=='{}'", credentials.getClass());
        return false;
    }

    /**
     * <p>Validate the token, has it enough fields and is the producing class as expected.
     * <b><u>Certainly not an acceptable method to use for real! This is just a demo.</u></b>
     * </p>
     *
     * @param simpleTokenCredentials
     * @param apparentEndpoint
     * @return
     */
    private boolean loginSimpleTokenCredentials(SimpleTokenCredentials simpleTokenCredentials, String apparentEndpoint) {

        String[] tokens = new String(simpleTokenCredentials.getToken(), StandardCharsets.UTF_8).split(",");
        if (tokens.length < QUADRUPLE) {
            LOGGER.error("login() tokens=='{}'", Arrays.asList(tokens));
            return false;
        }

        String theirBuildTimestamp = tokens[1];
        String kind = tokens[2];
        String module = tokens[3];

        if (!MyClientCredentialsFactory.class.getSimpleName().equals(kind)) {
            LOGGER.error("login() wrong kind for '{}'", Arrays.asList(tokens));
            return false;
        }

        // Tolerable for clients but not for servers
        if (!this.myBuildTimestamp.equals(theirBuildTimestamp)) {
            LOGGER.warn("login() my build '{}' different from their build in '{}'",
                this.myBuildTimestamp, Arrays.asList(tokens));
        }

        LOGGER.info("login() apparentEndpoint='{}', tokens=='{}'", apparentEndpoint, Arrays.asList(tokens));
        this.sharedState.put(Context.SECURITY_CREDENTIALS, module);
        return true;
    }

    /**
     * <p>Validate the password, is it based on the Ro13 of the user name.
     * <b><u>Certainly not an acceptable method to use for real! This is just a demo.</u></b>
     * </p>
     *
     * @param usernamePasswordCredentials
     * @param apparentEndpoint
     * @return
     */
    private boolean loginUsernamePasswordCredentials(UsernamePasswordCredentials usernamePasswordCredentials,
            String apparentEndpoint) {

        String unencryptedPassword = MyUtils.rot13(usernamePasswordCredentials.getPassword());
        boolean accept = usernamePasswordCredentials.getName().toLowerCase(Locale.ROOT)
                .equals(unencryptedPassword);

        for (String blockedPassword : this.blockedPasswords) {
            if (blockedPassword.equals(unencryptedPassword)) {
                accept = false;
                LOGGER.debug("Password in bloacked list");
            }
        }

        if (accept) {
            LOGGER.info("login(), apparentEndpoint='{}', credentials=='{}'", apparentEndpoint,
                    usernamePasswordCredentials);
            this.sharedState.put(Context.SECURITY_CREDENTIALS, usernamePasswordCredentials.getName());
            return true;
        } else {
            LOGGER.error("login(), incorrect password for credentials=='{}'",
                    usernamePasswordCredentials);
            return false;
        }
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
        String name = (String) this.sharedState.get(Context.SECURITY_CREDENTIALS);

        ClusterIdentityPrincipal clusterIdentityPrincipal = new ClusterIdentityPrincipal(name);
        ClusterRolePrincipal clusterRolePrincipal = new ClusterRolePrincipal(name);

        // For role based authorisation
        this.subject.getPrincipals().add(clusterRolePrincipal);

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

    /**
     * <p>Only log the blocked passwords once.
     * </p>
     */
    static boolean passwordLoggingOn() {
        return !loggedOnce;
    }
    /**
     * <p>Only log the blocked passwords once.
     * </p>
     */
    static void setPasswordLoggingOff() {
        loggedOnce = true;
    }
}
