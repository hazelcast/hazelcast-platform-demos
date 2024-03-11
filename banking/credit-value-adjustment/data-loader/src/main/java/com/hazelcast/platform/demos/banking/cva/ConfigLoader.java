/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

/**
 * <p>Load config data.</p>
 * <p>We don't need "{@code @Order}" annotation, won't matter
 * if this is before or after other maps.
 * </p>
 * <p>Do this after "{@link DataValidation}", as that produces much output.
 * </p>
 */
@Component
@Order(value = 5)
public class ConfigLoader implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String CPP_DOCKER = "cva-cpp";
    private static final String CPP_KUBERNETES = "cpp-service";
    private static final String CPP_LOCALHOST = "127.0.0.1";

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyProperties myProperties;

    /**
     */
    @Override
    public void run(String... args) throws Exception {
        IMap<String, String> cvaConfigMap = this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_CVA_CONFIG);

        cvaConfigMap.set(MyConstants.CONFIG_CPP_SERVICE_KEY, getLoadBalancer(this.myProperties.isUseViridian()));

        Set<String> keySet = new TreeSet<>(cvaConfigMap.keySet());
        keySet.forEach(key -> LOGGER.info("Config: '{}'=='{}'",
                key, cvaConfigMap.get(key)));
        LOGGER.info("{} entries in map '{}'", keySet.size(), cvaConfigMap.getName());
    }

    /**
     * <p>Find the Load Balancer to use to access C++, based on
     * system properties and derivation.
     * </p>
     *
     * @return Hostname, no port.
     */
    private static String getLoadBalancer(boolean useViridian) {
        String cppService = System.getProperty("my.cpp.service", "");
        boolean dockerEnabled =
                System.getProperty("my.docker.enabled", "false").equalsIgnoreCase(Boolean.TRUE.toString());
        boolean kubernetesEnabled =
                System.getProperty("my.kubernetes.enabled", "false").equalsIgnoreCase(Boolean.TRUE.toString());

        LOGGER.info("my.cpp.service=={}", cppService);
        LOGGER.info("dockerEnabled=={}", dockerEnabled);
        LOGGER.info("kubernetesEnabled=={}", kubernetesEnabled);
        LOGGER.info("use.viridian=={}", useViridian);

        // If set, validate but don't reject
        if (cppService.length() > 0) {
            validate(dockerEnabled, kubernetesEnabled, cppService, useViridian);
        } else {
            // Unset, so guess
            if (!dockerEnabled && !kubernetesEnabled) {
                cppService = CPP_LOCALHOST;
            }
            if (dockerEnabled && !kubernetesEnabled) {
                if (useViridian) {
                    String message = "Can't use Docker with Viridian for data loader, CVA-CPP service must be in cloud";
                    throw new RuntimeException(message);
                } else {
                    cppService = CPP_DOCKER;
                }
            }
            if (kubernetesEnabled) {
                cppService = CPP_KUBERNETES;
            }
        }
        LOGGER.info("cppService=={}", cppService);

        return cppService;
    }

    /**
     * <p>Validate how the C++ service is set, compared to localhost,
     * Docker or Kubernetes running.
     * </p>
     *
     * @param dockerEnabled False means localhost
     * @param kubernetesEnabled False means Docker or localhost
     * @param cppService The service URL
     * @param if true, service must be in the cloud
     */
    private static void validate(boolean dockerEnabled, boolean kubernetesEnabled, String cppService,
            boolean useViridian) {
        if (!dockerEnabled && !kubernetesEnabled && !cppService.equals(CPP_LOCALHOST)) {
            LOGGER.warn("localhost, but 'my.cpp.service'=='{}'", cppService);
        }
        if (dockerEnabled && !kubernetesEnabled && !cppService.equals(CPP_DOCKER)) {
            LOGGER.warn("Docker, but 'my.cpp.service'=='{}'", cppService);
        }
        if (kubernetesEnabled && !cppService.startsWith(CPP_KUBERNETES)) {
            if (!useViridian) {
                LOGGER.warn("Kubernetes, but 'my.cpp.service'=='{}'", cppService);
            }
        }
    }

}
