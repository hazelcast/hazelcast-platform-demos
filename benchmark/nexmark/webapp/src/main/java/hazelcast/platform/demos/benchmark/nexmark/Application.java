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

package hazelcast.platform.demos.benchmark.nexmark;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * <p>Entry point. Let Spring do the work.
 * </p>
 */
@SpringBootApplication
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
        System.exit(0);
    }

    /**
     * <p>For testing, make standalone cluster rather
     * then allow client to be autowired.
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "my", name = "mode", havingValue = "test")
    public HazelcastInstance hazelcastInstance() throws Exception {
        LOGGER.info("-=-=-=-=-");
        LOGGER.info("TEST MODE: Run as server not client");
        LOGGER.info("-=-=-=-=-");
        TimeUnit.SECONDS.sleep(1L);

        Config config = new Config();
        config.getJetConfig().setEnabled(true).setResourceUploadEnabled(true);
        config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true).setMembers(List.of("127.0.0.1"));
        return Hazelcast.newHazelcastInstance(config);
    }

}
