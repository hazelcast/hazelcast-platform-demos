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

package com.hazelcast.platform.demos.banking.cva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
@SpringBootApplication
@EnableConfigurationProperties(MyProperties.class)
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final int DEFAULT_PORT = 8080;

    private static int port = DEFAULT_PORT;

    /**
     * <p>Start a client of the specified cluster, and as this is
     * a webapp, leave it running for users to connect.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        String siteStr = System.getProperty("my.site");

        Tuple2<Integer, Site> handledArgs = MyUtils.twoArgsIntSite(args, Application.class.getName());

        // Override port if specified and sensible
        if (System.getProperty("server.port") != null) {
            LOGGER.info("System.getProperty(\"server.port\")=={}",
                    System.getProperty("server.port"));
            port = Integer.parseInt(System.getProperty("server.port"));
        } else {
           if (handledArgs.f0() != null && handledArgs.f0() > 0) {
               port = handledArgs.f0();
           }
        }

        // Site, System property wins if specified twice
        if (siteStr != null) {
            siteStr = (siteStr.equals(Site.SITE2.toString()) ? Site.SITE2.toString() : Site.SITE1.toString());
        } else {
            if (handledArgs.f1() != null) {
                siteStr = (handledArgs.f1() == Site.SITE2 ? Site.SITE2.toString() : Site.SITE1.toString());
            } else {
                siteStr = Site.SITE1.toString();
            }
        }

        System.setProperty("my.site", siteStr);
        System.setProperty("server.port", String.valueOf(port));
        LOGGER.info("Selected port {}/site, site '{}'", port, siteStr);

        SpringApplication.run(Application.class);
    }

}
