/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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
import java.util.Properties;
import java.util.UUID;

import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.config.YamlJetConfigBuilder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
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
                String publicAddress = System.getProperty("hazelcast.local.publicAddress");
                String dockerHost = publicAddress.split(":")[0];
                String port = publicAddress.split(":")[1];
                tcpIpConfig.setMembers(Arrays.asList(dockerHost + ":5701", dockerHost + ":5702", dockerHost + ":5703"));
                jetConfig.getHazelcastConfig().getNetworkConfig().setPort(Integer.parseInt(port));
            } else {
                tcpIpConfig.setMembers(Arrays.asList("127.0.0.1"));
            }

            joinConfig.setTcpIpConfig(tcpIpConfig);

            LOGGER.info("Non-Kubernetes configuration: member-list: {}",
                    tcpIpConfig.getMembers());
        }

        return jetConfig;
    }

    /**
     * <p>Control properties for the Kafka connector.
     * </p>
     * <ul>
     * <li><p><i>AUTO_OFFSET_RESET_CONFIG</i> - where to begin reading from.
     * <p></li>
     * <li><p><i>BOOTSTRAP_SERVERS_CONFIG</i> - the list of brokers to connect to.
     * <p></li>
     * <li><p><i>GROUP_ID_CONFIG</i> - The Id for the Jet job connecting to Kafka,
     * make it unique rather than rely on Kafka generating one.
     * <p></li>
     * <li><p><i>KEY_DESERIALIZER_CLASS_CONFIG</i> - how to de-serialize the message key.
     * <p></li>
     * <li><p><i>VALUE_DESERIALIZER_CLASS_CONFIG</i> - how to de-serialize the message value.
     * <p></li>
     * </ul>
     *
     * @param bootstrapServers A CSV list of brokers
     * @return Properties used by the Kafka connector
     */
    public static Properties kafkaSourceProperties(String bootstrapServers) {

        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());

        return properties;
    }

}
