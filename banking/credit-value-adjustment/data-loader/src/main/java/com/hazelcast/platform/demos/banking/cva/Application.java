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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

/**
 * <p>Entry point, "{@code main()}" method. Takes an
 * optional command line or Java system property to determine
 * whether to connect to "{@code CVA_SITE1}" or "{@code CVA_SITE2}".
 * </p>
 */
@SpringBootApplication
@EnableConfigurationProperties(MyProperties.class)
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    /**
     * <p>Start a client of the specified cluster, and once the data
     * loader classes have finished, shut down.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        String siteStr = System.getProperty("my.site");

        if (siteStr == null) {
            if (args.length == 0) {
                siteStr = Site.CVA_SITE1.toString();
                LOGGER.info("No args, site=='{}'", siteStr);
            } else {
                siteStr = args[0].equals(Site.CVA_SITE1.toString()) ? Site.CVA_SITE1.toString() : Site.CVA_SITE2.toString();
                LOGGER.info("Arg='{}', site=='{}'", args[0], siteStr);
            }

            System.setProperty("my.site", siteStr);
        }

        SpringApplication.run(Application.class, args);
        System.exit(0);
    }

}
