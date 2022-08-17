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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.stub.StreamObserver;

/**
 * <p>Tests a single send down a bi-directional gRPC channel.
 * </p>
 * <p>
 * <b>Note:</b> No Hazelcast or Spring, standard Java
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final String DATAFILE = "testdata.json";
    private static final long ONE_HUNDRED_MS = 100L;
    // Needs to match C++ gRPC Server.
    private static final int PORT = 30001;
    private static final int FIVE_ATTEMPTS = 5;
    private static final int TEN_THOUSAND = 10_000;

    private static AtomicBoolean exceptions = new AtomicBoolean(false);
    private static AtomicLong pendingResponses = new AtomicLong(0);

    /**
     * <p>
     * Run the {@link ApplicationRunner} then shutdown.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        String datum = Files.readAllLines(Paths.get(File.separator + DATAFILE)).get(0);
        String host = getTargetHost();

        LOGGER.info("Channel to target host: {}", host);
        ManagedChannel managedChannel = getManagedChannelBuilder(host, PORT).build();
        JetToCppGrpc.JetToCppStub jetToCppStub = JetToCppGrpc.newStub(managedChannel);

        // Input batch contains one data item
        InputMessage.Builder inputMessageBuilder = InputMessage.newBuilder();
        inputMessageBuilder.addInputValue(datum);
        InputMessage inputMessage = inputMessageBuilder.build();

        // Callback handler for output from bi-directional stream
        StreamObserver<OutputMessage> responseObserver = responseObserver();
        StreamObserver<InputMessage> requestObserver = jetToCppStub.streamingCall(responseObserver);

        // Sending batch async
        pendingResponses.incrementAndGet();
        requestObserver.onNext(inputMessage);

        // Wait for responses
        try {
            while (pendingResponses.get() > 0 && !exceptions.get()) {
                LOGGER.info("Sleeping waiting for async response");
                TimeUnit.MILLISECONDS.sleep(ONE_HUNDRED_MS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        requestObserver.onCompleted();
        responseObserver.onCompleted();
        managedChannel.shutdown();
    }

    /**
     * <p>Callback handle for responses. Count down a success message
     * or a failure counter.
     * </p>
     *
     * @return
     */
    private static StreamObserver<OutputMessage> responseObserver() {
        return new StreamObserver<OutputMessage>() {
            @Override
            public void onNext(OutputMessage outputMessage) {
                LOGGER.info("requestObserver.onNext() - received batch of {}",
                        outputMessage.getOutputValueCount());
                pendingResponses.decrementAndGet();
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("requestObserver.onError()", t);
                exceptions.set(true);
            }

            @Override
            public void onCompleted() {
            }
        };
    }
    /**
     * <p>
     * Determine target host. If provided with an IP (which may be a load balancer
     * onto a Kubernetes service or Docker node), use that. Otherwise assume using a
     * Kubernetes service directly.
     * <p>
     *
     * @return IP or StatefulSet DNS
     */
    private static String getTargetHost() {
        // Set in docker-cpp-tester.sh or Kubernetes YAML
        String hostIp = System.getProperty("host.ip", "");

        if (hostIp.isBlank()) {
            // StatefulSet name
            return "cva-cpp.default.svc.cluster.local";
        } else {
            return hostIp;
        }
    }

    public static ManagedChannelBuilder<?> getManagedChannelBuilder(String host, int port) {
        ManagedChannelBuilder<?> managedChannelBuilder =
                // ManagedChannelBuilder.forAddress(host, port);
                // Use Netty directly for "withOption"
                NettyChannelBuilder.forAddress(host, port).withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS,
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

        return managedChannelBuilder;    }

}
