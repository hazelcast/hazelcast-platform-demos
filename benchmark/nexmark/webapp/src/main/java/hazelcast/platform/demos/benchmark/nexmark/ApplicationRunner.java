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

package hazelcast.platform.demos.benchmark.nexmark;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.platform.demos.utils.UtilsFormatter;

/**
 * <p>Mainly leave it up to React.js
 * </p>
 */
@Configuration
public class ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
    private static final long TEN_THOUSAND = 10_000L;
    private static final long FIVE_HUNDRED = 500L;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Value("${my.autostart.q05}")
    private String myAutostartQ05;
    @Value("${spring.application.name}")
    private String springApplicationName;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            LOGGER.info("-=-=-=-=- '{}' START -=-=-=-=-=-", this.springApplicationName);

            boolean ok = this.init();

            if (ok) {
                if (!this.myAutostartQ05.isBlank()) {
                    this.autostart();
                }

                try {
                    while (true) {
                        TimeUnit.MINUTES.sleep(1L);
                        LOGGER.info("-=-=-=-=- MAPS -=-=-=-=-=-");
                        Collection<String> names = List.of(BenchmarkBase.IMAP_NAME_CURRENT_LATENCIES,
                                BenchmarkBase.IMAP_NAME_MAX_LATENCIES);
                        for (String name : names) {
                            Set<Entry<Object, Object>> entrySet =
                                    this.hazelcastInstance.getMap(name).entrySet();
                            entrySet.forEach(entry -> {
                                LOGGER.info("MAP '{}' : {}", name, entry);
                            });
                        }
                        LOGGER.info("-=-=-=-=- JOBS -=-=-=-=-=-");
                        this.hazelcastInstance.getJet().getJobs()
                        .forEach(job -> {
                            LOGGER.info("JOB '{}' : {} : {}",
                                    Objects.toString(job.getName()),
                                    job.getStatus(), job);
                        });
                        LOGGER.info("-=-=-=-=-");
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn("Interrupted: {}", e.getMessage());
                }
            }

            LOGGER.info("-=-=-=-=- '{}' END -=-=-=-=-=-", this.springApplicationName);
        };
    }

    /**
     * <p>Define mappings and views. Map have same structure for maximum and current
     * latency.
     * </p>
     * @return
     */
    private boolean init() {
        String mappingBody = "("
                + "    \"kind\" VARCHAR EXTERNAL NAME \"__key." + BenchmarkBase.PROP_KIND + "\","
                + "    \"start_timestamp\" BIGINT EXTERNAL NAME \"__key.start_timestamp\","
                + "    \"start_timestamp_str\" VARCHAR EXTERNAL NAME \"__key.start_timestamp_str\","
                + "    \"processing_guarantee\" VARCHAR EXTERNAL NAME \"__key." + BenchmarkBase.PROP_PROCESSING_GUARANTEE + "\","
                + "    \"events_per_second\" VARCHAR EXTERNAL NAME \"__key." + BenchmarkBase.PROP_EVENTS_PER_SECOND + "\","
                + "    \"num_distinct_keys\" BIGINT EXTERNAL NAME \"__key." + BenchmarkBase.PROP_NUM_DISTINCT_KEYS + "\","
                + "    \"sliding_step_millis\" BIGINT EXTERNAL NAME \"__key." + BenchmarkBase.PROP_SLIDING_STEP_MILLIS + "\","
                + "    \"window_size_millis\" BIGINT EXTERNAL NAME \"__key." + BenchmarkBase.PROP_WINDOW_SIZE_MILLIS + "\","
                + "    \"timestamp\" BIGINT EXTERNAL NAME \"this.timestamp\","
                + "    \"timestamp_str\" VARCHAR EXTERNAL NAME \"this.timestamp_str\","
                + "    latency_ms BIGINT EXTERNAL NAME \"this.latency_ms\","
                + "    offset_ms BIGINT EXTERNAL NAME \"this.offset_ms\""
                + ")"
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'json-flat',"
                + " 'valueFormat' = 'json-flat'"
                + " )";

        String viewBody = "AS SELECT"
                + "  JSON_VALUE(CAST(__key AS VARCHAR), '$." + BenchmarkBase.PROP_KIND  + "')"
                + " AS \"" + BenchmarkBase.PROP_KIND + "\""
                + ", JSON_VALUE(CAST(__key AS VARCHAR), '$." + BenchmarkBase.PROP_EVENTS_PER_SECOND  + "')"
                + " AS \"" + BenchmarkBase.PROP_EVENTS_PER_SECOND + "\""
                + ", JSON_VALUE(CAST(this AS VARCHAR), '$.latency_ms' RETURNING BIGINT)"
                + " AS \"latency_ms\" "
                ;

        boolean ok = this.defineMappingAndView(BenchmarkBase.IMAP_NAME_CURRENT_LATENCIES, mappingBody, viewBody);
        ok = ok & this.defineMappingAndView(BenchmarkBase.IMAP_NAME_MAX_LATENCIES, mappingBody, viewBody);
        return ok;
    }

    private boolean defineMappingAndView(String mapName, String mappingBody, String viewBody) {
        String mapping = "CREATE OR REPLACE MAPPING \"" + mapName + "\" " + mappingBody;
        String view = "CREATE OR REPLACE VIEW \"" + mapName + "_VIEW\" " + viewBody
                + " FROM \"" + mapName + "\"";
        for (String definition : List.of(mapping, view)) {
            try {
                LOGGER.debug("Definition '{}'", definition);
                this.hazelcastInstance.getSql().execute(definition);
            } catch (Exception e) {
                LOGGER.error(mapping, e);
                return false;
            }
        }
        // If definitions were ok, ensure target object exists
        this.hazelcastInstance.getMap(mapName);
        return true;
    }

    /**
     * <p>If a job is running, cancel it and that's it. Otherwise
     * kick of Q05 with pre-set params, same as "{@code JobSub.tsx}".
     * So run the webapp once to start and once again to stop the
     * default job.
     * </p>
     */
    private void autostart() {
        LOGGER.debug("@@@@@@@");
        LOGGER.debug("@@@@@@@ <====>");
        LOGGER.debug("@@@@@@@ autostart() @@@@@@@");
        Collection<Job> jobs = this.hazelcastInstance.getJet().getJobs();
        final AtomicLong cancelled = new AtomicLong(0L);
        if (!jobs.isEmpty()) {
            jobs.forEach(job -> {
                if (job.getStatus() == JobStatus.RUNNING) {
                    LOGGER.info("STOPPING JOB '{}'", job);
                    job.cancel();
                    cancelled.incrementAndGet();
                } else {
                    if (job.getStatus() == JobStatus.FAILED) {
                        LOGGER.info("SKIP \"{}\" (CANCELLED?) JOB '{}'", job.getStatus(), job);
                    } else {
                        LOGGER.error("SKIP \"{}\" JOB '{}'", job.getStatus(), job);
                    }
                }
            });
        }

        if (cancelled.get() == 0) {
            Q05HotItems q05HotItems = new Q05HotItems();

            Map<String, Long> params = new TreeMap<>();
            params.put(BenchmarkBase.PROP_EVENTS_PER_SECOND, Long.parseLong(this.myAutostartQ05));
            params.put(BenchmarkBase.PROP_NUM_DISTINCT_KEYS, TEN_THOUSAND);
            params.put(BenchmarkBase.PROP_SLIDING_STEP_MILLIS, FIVE_HUNDRED);
            params.put(BenchmarkBase.PROP_WINDOW_SIZE_MILLIS, TEN_THOUSAND);

            long now = System.currentTimeMillis();
            String jobNameSuffix = "@" + UtilsFormatter.timestampToISO8601(now);

            ProcessingGuarantee processingGuarantee = ProcessingGuarantee.NONE;

            Job job = q05HotItems.run(this.hazelcastInstance, jobNameSuffix, now, params, processingGuarantee);
            LOGGER.info("STARTED JOB '{}' WITH '{}'=={}", job, BenchmarkBase.PROP_EVENTS_PER_SECOND,
                    String.format("%,d", params.get(BenchmarkBase.PROP_EVENTS_PER_SECOND)));
        }

        LOGGER.debug("@@@@@@@ autostart() @@@@@@@");
        LOGGER.debug("@@@@@@@ <====>");
        LOGGER.debug("@@@@@@@");
    }

}
