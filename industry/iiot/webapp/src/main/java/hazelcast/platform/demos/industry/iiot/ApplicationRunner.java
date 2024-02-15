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

package hazelcast.platform.demos.industry.iiot;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Kick off some listeners then mainly leave it up to React.js
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationRunner {
    private static final int TEN = 10;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private MyConfigHandler myConfigHandler;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                this.hazelcastInstance.getName());

            // Ensure expected config has something
            this.myConfigHandler.initConfig();

            // Listeners
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_CONFIG)
                .addEntryListener(this.myConfigHandler, true);
            MySlf4jListener mySlf4jListener = new MySlf4jListener(this.simpMessagingTemplate);
            this.hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYS_LOGGING)
                .addEntryListener(mySlf4jListener, true);

            // Send periodically to React in case socket notification missed (eg. at startup)
            while (this.hazelcastInstance.getLifecycleService().isRunning()) {
                this.myConfigHandler.pushConfig();
                TimeUnit.SECONDS.sleep(TEN);
            }

            // Reached if shutdown from Management Center
            log.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-",
                    this.hazelcastInstance.getName());
        };
    }

}
