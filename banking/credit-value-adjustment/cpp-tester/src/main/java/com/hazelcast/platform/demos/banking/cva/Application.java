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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 * <p>Tests a single send down a bi-directional gRPC channel.
 * </p>
 * <p>
 * <b>Note:</b> No Hazelcast or Spring, standard Java.
 * </p>
 * <p>See also module <i>cpp-tester2</i> which uses a simple
 * Jet job to test the pipeline.
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final String DATAFILE = "testdata.json";
    private static final long ONE_HUNDRED_MS = 100L;

    private static AtomicBoolean exceptions = new AtomicBoolean(false);
    private static AtomicLong pendingResponses = new AtomicLong(0);

    /**
     * <p>Send and receive, then shutdown.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("=== START ===");
        LOGGER.info("Runtime.getRuntime().availableProcessors()=={}", Runtime.getRuntime().availableProcessors());
        String datum = Files.readAllLines(Paths.get(File.separator + DATAFILE)).get(0);
        String host = getTargetHost();

        LOGGER.info("Channel to target host: {}", host);
        ManagedChannel managedChannel = MyUtils.getManagedChannelBuilder(host).build();
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
        LOGGER.info("Sending batch of {}", inputMessage.getInputValueCount());
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
        LOGGER.info("===  END  ===");
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
                LOGGER.info("responseObserver.onNext() - received batch of {}",
                        outputMessage.getOutputValueCount());
                for (int i = 0; i < outputMessage.getOutputValueCount(); i++) {
                    LOGGER.info(" {} - {}", i, outputMessage.getOutputValue(i));
                }
                pendingResponses.decrementAndGet();
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("responseObserver.onError()", t);
                exceptions.set(true);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("responseObserver.onCompleted()");
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

}
