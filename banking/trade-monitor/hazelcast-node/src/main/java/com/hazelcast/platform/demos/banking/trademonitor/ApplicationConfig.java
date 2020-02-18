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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.Arrays;

import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.config.YamlJetConfigBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Non-default configuration for Jet, to allow this example to run in Kubernetes (by default),
 * in Docker or as a stand-alone Java.
 * </p>
 */
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    /**
     * <p>Create the configuration for Jet, using any default files found on the classpath.
     * These are "{@code hazelcast-jet.yml}" for Jet, and "{@code hazelcast.yml}" for the
     * IMDG instance embedded in Jet. You can also do this with XML instead of YAML if that
     * is your preference.
     * </p>
     * <p>The default behaviour, specified in "{@code hazelcast.yml}" is for Kubernetes,
     * so to run outside Kubernetes set the environment variable "{@code my.kubernetes.enabled}"
     * to "{@code false}" and the configuration loaded from the YAML file will be amended
     * for TCP based discovery, which works on Docker and normal hosts. Refer to <b>README.md</b>
     * for more details.
     * </p>
     */
    public static JetConfig buildJetConfig() {
        JetConfig jetConfig = new YamlJetConfigBuilder().build();

        JoinConfig joinConfig = jetConfig.getHazelcastConfig().getNetworkConfig().getJoin();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("Kubernetes configuration: service-dns: {}",
                    joinConfig.getKubernetesConfig().getProperty("service-dns"));
        } else {
            joinConfig.getKubernetesConfig().setEnabled(false);

            TcpIpConfig tcpIpConfig = new TcpIpConfig();
            tcpIpConfig.setEnabled(true);
            if (System.getProperty("hazelcast.local.publicAddress", "").length() != 0) {
                tcpIpConfig.setMembers(Arrays.asList(System.getProperty("hazelcast.local.publicAddress")));
            } else {
                tcpIpConfig.setMembers(Arrays.asList("127.0.0.1"));
            }

            joinConfig.setTcpIpConfig(tcpIpConfig);

            LOGGER.info("Non-Kubernetes configuration: member-list: {}",
                    tcpIpConfig.getMembers());
        }

        return jetConfig;
    }

}
