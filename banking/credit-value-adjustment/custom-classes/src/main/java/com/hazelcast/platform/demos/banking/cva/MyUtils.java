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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.platform.demos.banking.cva.MyConstants.Site;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;

/**
 * <p>Utility functions that may be useful to more than one module.
 * </p>
 */
public class MyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyUtils.class);

    private static final int PORT_RANGE = 1000;
    // Needs to match C++ gRPC Server.
    private static final int PORT = 30001;
    private static final int FIVE_ATTEMPTS = 5;
    private static final int TEN_THOUSAND = 10_000;

    /**
     * <p>When trying to run two clusters on the same host,
     * keep the port ranges apart.
     * </p>
     * <p>Put the first cluster on the default port, to make
     * it easy for Management Center to connect.
     * </p>
     *
     * @param site "{@code SITE1}" or "{@code SITE2}"
     * @return 5701 or 6701.
     */
    public static int getLocalhostBasePort(Site site) {
        int multiplier = (site == Site.SITE1) ? 0 : 1;
        return NetworkConfig.DEFAULT_PORT + multiplier * PORT_RANGE;
    }

    /**
     * <p>Handle two args on the command line, the site and a
     * numeric (eg. port). Either, neither or both may be
     * specified, and if both allow any order.
     * </p>
     *
     * @param args A String array of unknown size
     * @param prefix For logging output
     * @return An int and a {@link Site}
     */
    public static Tuple2<Integer, Site> twoArgsIntSite(String[] args, String prefix) {
        if (args.length > 2) {
            String[] ignored = Arrays.copyOfRange(args, 2, args.length);
            LOGGER.warn("{}: Ignoring {} excess arg{}: {}",
                    prefix, ignored.length, (ignored.length == 1 ? "" : "s"), ignored);
        }

        Integer integerValue = null;
        boolean intUsed = false;
        Site siteValue = null;
        boolean siteUsed = false;

        for (String arg : Arrays.copyOf(args,  2)) {
            if (arg != null && arg.length() > 0) {
                // Try as numeric
                if (!intUsed) {
                    try {
                        integerValue = Integer.parseInt(arg);
                        intUsed = true;
                    } catch (NumberFormatException ignored) {
                    }
                }

                // Try as Site ENUM
                if (!siteUsed) {
                    if (arg.equals(Site.SITE1.toString())) {
                        siteValue = Site.SITE1;
                        siteUsed = true;
                    }
                    if (arg.equals(Site.SITE2.toString())) {
                        siteValue = Site.SITE2;
                        siteUsed = true;
                    }
                }
            }
        }

        return Tuple2.tuple2(integerValue, siteValue);
    }


    /**
     * <p>Test if another job is doing the same thing at the same
     * time, to avoid swamping the system. We don't want
     * "{@code CALCULATE_EXPOSURE@10AM}" to run if
     * "{@code CALCULATE_EXPOSURE@9AM}" is still going.
     * </p>
     * <p>This method isn't bulletproof. It relies on jobs having
     * a name and this following a naming pattern.
     * </p>
     * <p>Also, since we don't (and don't want to) suspend processing
     * while we decide, we may spot at job that is running now but
     * actually finishes by the time we want to submit our clone.
     * </p>
     *
     * @param prefix The start of a job name
     * @param hazelcastInstance To access the job registry
     * @return The first match, or null if none.
     */
    public static Job findRunningJobsWithSamePrefix(String prefix, HazelcastInstance hazelcastInstance) {
        Job match = null;

        for (Job job : hazelcastInstance.getJet().getJobs()) {
            if (job.getName() != null && job.getName().startsWith(prefix)) {
                if ((job.getStatus() == JobStatus.STARTING)
                     || (job.getStatus() == JobStatus.RUNNING)
                     || (job.getStatus() == JobStatus.SUSPENDED)
                     || (job.getStatus() == JobStatus.SUSPENDED_EXPORTING_SNAPSHOT)) {
                    return job;
                }
            }
        }

        return match;
    }

    /**
     * <p>Take a timestamp {@code long} and convert it into an ISO-8601
     * style string, but drop the millisecond accuracy.
     * </p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO-8601</a>
     * @param timestamp From "{@code System.currentTimeMillis()}" probabaly
     * @return Time as a string
     */
    public static String timestampToISO8601(long timestamp) {
        LocalDateTime localDateTime =
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

        String timestampStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);

        if (timestampStr.indexOf('.') > 0) {
            timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
        }

        return timestampStr;
    }

    /**
     * <p>Construct and configure the gRPC connection, see
     * <a href="https://github.com/grpc/proposal/blob/master/A6-client-retries.md#retry-policy-capabilities">
     * retry-policy-capabilities</a> and
     * <a href=""
     * </p>
     *
     * @param host A load balancer DNS name
     * @return
     */
    public static ManagedChannelBuilder<?> getManagedChannelBuilder(String host) {
        LOGGER.info("ManagedChannelBuild for '{}'", host);

        ManagedChannelBuilder<?> managedChannelBuilder =
                // ManagedChannelBuilder.forAddress(host, port);
                // Use Netty directly for "withOption"
                NettyChannelBuilder.forAddress(host, PORT).withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        TEN_THOUSAND);

        // No SSL, only a demo.
        managedChannelBuilder.usePlaintext();

        // May retries for each RPC
        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", Double.valueOf(FIVE_ATTEMPTS));
        retryPolicy.put("initialBackoff", "0.2s");
        retryPolicy.put("maxBackoff", "10s");
        retryPolicy.put("backoffMultiplier", Double.valueOf(2));
        retryPolicy.put("retryableStatusCodes", List.of("RESOURCE_EXHAUSTED"));

        Map<String, Object> methodConfig = new HashMap<>();
        Map<String, Object> name = new HashMap<>();
        name.put("service", "com_hazelcast_platform_demos_banking_cva.JetToCpp");
        name.put("method", "streamingCall");

        methodConfig.put("name", List.of(name));
        methodConfig.put("retryPolicy", retryPolicy);

        Map<String, Object> serviceConfig = new HashMap<>();
        serviceConfig.put("methodConfig", List.of(methodConfig));

        managedChannelBuilder.defaultServiceConfig(serviceConfig);

        // Deactivates stats
        managedChannelBuilder.enableRetry();

        // Don't use - May not be fully implemented. Max retries for all RPCs
        //managedChannelBuilder.maxRetryAttempts(3);

        return managedChannelBuilder;
    }

}
