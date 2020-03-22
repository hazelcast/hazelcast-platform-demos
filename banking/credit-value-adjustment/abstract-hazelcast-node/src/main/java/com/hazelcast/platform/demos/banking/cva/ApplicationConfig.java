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

package com.hazelcast.platform.demos.banking.cva;

import java.util.List;

import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.config.YamlJetConfigBuilder;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Non-default configuration for Jet, to allow this example to run in Kubernetes (by default),
 * in Docker or as a stand-alone Java.
 * </p>
 */
@Configuration
public class ApplicationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

    private final Site site;

    public ApplicationConfig(MyProperties myProperties) {
        // From Maven, application.yml, wouldn't be in environment by default.
        System.setProperty("my.site", myProperties.getSite().toString());
        this.site = myProperties.getSite();
    }

    /**
     * <p>Create the configuration for Jet using "{@code *.yml}" files found on the classpath.
     * On a <i>cloud-first</i> approach, these are configured for Kubernetes.
     * </p>
     * <p>If the "{@code my.kubernetes.enabled}" indicates we are not in Kubernetes,
     * change the configuration to use either localhost or the provided network.
     * </p>
     * <p>See <b>README.md</b> and "{@code src/main/scripts}" in the project top-level.
     * </p>
     */
    @Bean
    public JetConfig jetConfig() {
        JetConfig jetConfig = new YamlJetConfigBuilder().build();

        NetworkConfig networkConfig = jetConfig.getHazelcastConfig().getNetworkConfig();

        if (System.getProperty("my.kubernetes.enabled", "").equals("true")) {
            LOGGER.info("Kubernetes configuration: service-dns: {}",
                    networkConfig.getJoin().getKubernetesConfig().getProperty("service-dns"));
        } else {
            networkConfig.getJoin().getKubernetesConfig().setEnabled(false);

            int port = MyUtils.getLocalhostBasePort(this.site);

            networkConfig.setPort(port);

            TcpIpConfig tcpIpConfig = new TcpIpConfig();
            tcpIpConfig.setEnabled(true);
            String host = System.getProperty("hazelcast.local.publicAddress", "127.0.0.1");
            List<String> memberList = List.of(host + ":" + port,
                    host + ":" + (port + 1), host + (port + 2));
            tcpIpConfig.setMembers(memberList);

            networkConfig.getJoin().setTcpIpConfig(tcpIpConfig);

            LOGGER.info("Non-Kubernetes configuration: member-list: {}",
                    tcpIpConfig.getMembers());
        }

        return jetConfig;
    }

    /**
     * <p>Create a JetInstance from the supplied configuration object,
     * loaded from "{@code hazelcast.yml}", potentially "{@code hazelcast-jet.yml}"
     * and potentially amended in flight by Java method above.
     * </p>
     *
     * @param jetConfig Created above
     * @return A JetInstance, created now
     */
    @Bean
    public JetInstance jetInstance(JetConfig jetConfig) {
        return Jet.newJetInstance(jetConfig);
    }
}
