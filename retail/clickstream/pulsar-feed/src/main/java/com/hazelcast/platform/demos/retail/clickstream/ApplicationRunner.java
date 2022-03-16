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

package com.hazelcast.platform.demos.retail.clickstream;

import org.apache.pulsar.client.api.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.jet.datamodel.Tuple2;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Wrap the actual writing with logging.
 * </p>
 */
@Slf4j
@Configuration
public class ApplicationRunner {

    @Autowired
    private LoaderService loaderService;
    @Autowired
    private MyProperties myProperties;
    @Autowired
    private Producer<String> producer;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("-=-=-=-=- START build '{}' by '{}' START -=-=-=-=-=-",
                this.myProperties.getBuildTimestamp(), this.myProperties.getBuildUserName());

            long before = System.currentTimeMillis();

            // MyConstants.CSV_INPUT_FILE1 - the "ordered" column is always 0
            String[] files = new String[] { MyConstants.CSV_INPUT_FILE2 };
            @SuppressWarnings("unchecked")
            Tuple2<Integer, String>[] counts = new Tuple2[files.length];

            for (int i = 0 ; i < files.length ; i++) {
                counts[i] = this.loaderService.readCsvWritePulsar(files[i], this.producer);
            }

            long elapsed = System.currentTimeMillis() - before;

            int total = 0;
            for (int i = 0 ; i < files.length ; i++) {
                total += (counts[i].f0() == null ? 0 : counts[i].f0());
                log.info("{}: {}",
                        this.loaderService.getClass().getSimpleName(), counts[i].f1());
            }

            log.info("{}, wrote {} in {}ms",
                    this.loaderService.getClass().getSimpleName(), total, elapsed);

            log.info("-=-=-=-=-  END  build '{}' by '{}'  END  -=-=-=-=-=-",
                    this.myProperties.getBuildTimestamp(), this.myProperties.getBuildUserName());
       };
    }
}
