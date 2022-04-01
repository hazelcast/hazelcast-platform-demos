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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.platform.demos.utils.UtilsFormatter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>REST end-points for launching the benchmarks
 * </p>
 */
@RestController
@RequestMapping("/rest")
public class MyRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRestController.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @GetMapping(value = "/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Class.forName() can throw exceptions")
    public String submit(
            @RequestParam("kind") String kind,
            @RequestParam("eventsPerSecond") int eventsPerSecond,
            @RequestParam("windowSizeMillis") int windowSizeMillis
            ) {
        LOGGER.info("submit(kind='{}', eventsPerSecond={}, windowSizeMillis={})",
                kind, eventsPerSecond, eventsPerSecond, windowSizeMillis);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append(" \"kind\" : \"").append(kind).append("\"");

        try {
            String pkgName = BenchmarkBase.class.getPackage().getName();
            BenchmarkBase benchmark = (BenchmarkBase)
                    Class.forName(pkgName + '.' + kind).getDeclaredConstructor().newInstance();

            long now = System.currentTimeMillis();
            String jobNameSuffix = "@" + UtilsFormatter.timestampToISO8601(now);

            Job job = benchmark.run(this.hazelcastInstance, jobNameSuffix, eventsPerSecond, windowSizeMillis);

            stringBuilder.append(", \"id\": \"" + job.getId() + "\"");
            stringBuilder.append(", \"name\": \"" + job.getName() + "\"");
            stringBuilder.append(", \"error\": " + false);
            stringBuilder.append(", \"error_message\": \"\"");
        } catch (Exception e) {
            stringBuilder.append(", \"id\": \"\"");
            stringBuilder.append(", \"name\": \"\"");
            stringBuilder.append(", \"error\": " + true);
            stringBuilder.append(", \"error_message\": \"" + UtilsFormatter.safeForJsonStr(e.getMessage()) + "\"");
            LOGGER.error("Exception for " + kind, e);
        }

        stringBuilder.append(" }");
        return stringBuilder.toString();
    }

}
