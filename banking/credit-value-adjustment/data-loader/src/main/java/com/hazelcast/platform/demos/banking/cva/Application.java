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

import com.hazelcast.jet.datamodel.Tuple2;
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

    private static int threshold = -1;

    /**
     * <p>Start a client of the specified cluster, and once the data
     * loader classes have finished, shut down.
     * </p>
     * <p>Has two arguments, both optional in any order, one text one numeric:</p>
     * <ul>
     * <li><b>site</b> Select "{@code CVA_SITE1}" "{@code CVA_SITE2}", default "{@code CVA_SITE1}"
     * <li><b>threshold</b> Record limit per map for upload, default unlimited
     * </ul>
     */
    public static void main(String[] args) throws Exception {
        String siteStr = System.getProperty("my.site");

        Tuple2<Integer, Site> handledArgs = MyUtils.twoArgsIntSite(args, Application.class.getName());

        // Override threshold if specified and sensible
        if (handledArgs.f0() != null && handledArgs.f0() > 0) {
            threshold = handledArgs.f0();
        }

        // Site, System property wins if specified twice
        if (siteStr != null) {
            siteStr = (siteStr.equals(Site.CVA_SITE2.toString()) ? Site.CVA_SITE2.toString() : Site.CVA_SITE1.toString());
        } else {
            if (handledArgs.f1() != null) {
                siteStr = (handledArgs.f1() == Site.CVA_SITE2 ? Site.CVA_SITE2.toString() : Site.CVA_SITE1.toString());
            } else {
                siteStr = Site.CVA_SITE1.toString();
            }
        }

        System.setProperty("my.site", siteStr);
        LOGGER.info("Upload threshold {}/site, site '{}'", threshold, siteStr);

        SpringApplication.run(Application.class);
        System.exit(0);
    }


    /**
     * <p>Return the maximum number of lines to upload from each file.
     * We probably can't accommodate billions of intermediate results
     * when developing on a laptop.
     * </p>
     *
     * @return 0 for unlimited, a positive integer in all cases.
     */
    public static int getThreshold() {
        return threshold;
    }

}
