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

package hazelcast.platform.demos.industry.iiot;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Run for a while.
 * </p>
 * <p>Other scheduled tasks such as {@link StatsRunnable}
 * will produce intermittent output.
 * </p>
 */
@Configuration
@Slf4j
public class ApplicationRunner {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("-=-=-=-=- START '{}' START -=-=-=-=-=-",
                this.hazelcastInstance.getName());

            TimeUnit.HOURS.sleep(1L);

            log.info("-=-=-=-=-  END  '{}'  END  -=-=-=-=-=-",
                    this.hazelcastInstance.getName());
        };
    }


}
