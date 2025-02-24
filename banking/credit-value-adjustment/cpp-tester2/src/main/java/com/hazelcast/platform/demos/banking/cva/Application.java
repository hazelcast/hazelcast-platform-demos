/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.grpc.GrpcService;
import com.hazelcast.jet.grpc.GrpcServices;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.TestSources;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 * <p>Tests a single send down a bi-directional gRPC channel.
 * </p>
 * <p>
 * <b>Note:</b> Hazelcast Jet used, otherwise the same as module <i>cpp-tester</i>.
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final String DATAFILE = "testdata.json";
    private static final long ONE_HUNDRED_MS = 100L;

    /**
     * <p>Create a Hazelcast cluster of one node, submit the Jet
     * job, and once it finishes shut down.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("=== START ===");
        LOGGER.info("Runtime.getRuntime().availableProcessors()=={}", Runtime.getRuntime().availableProcessors());
        String datum = Files.readAllLines(Paths.get(File.separator + DATAFILE)).get(0);
        String host = getTargetHost();

        LOGGER.info("Channel to target host: {}", host);

        Config config = new Config();
        config.getJetConfig().setEnabled(true);
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        Pipeline pipeline = build(host, datum);

        Job job = hazelcastInstance.getJet().newJob(pipeline);
        JobStatus jobStatus = job.getStatus();
        while (jobStatus != JobStatus.COMPLETED && jobStatus != JobStatus.FAILED) {
            LOGGER.info("Sleeping waiting for job status, was: {}", jobStatus);
            TimeUnit.MILLISECONDS.sleep(ONE_HUNDRED_MS);
            jobStatus = job.getStatus();
        }

        hazelcastInstance.shutdown();
        LOGGER.info("===  END  ===");
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

    /**
     * <p>Jet job to send one item in a batch
     * </p>
     *
     * @param host For GRPC
     * @param datum To send
     * @return
     */
    private static Pipeline build(String host, String datum) {
        FunctionEx<? super ManagedChannel,
                ? extends FunctionEx<StreamObserver<OutputMessage>, StreamObserver<InputMessage>>>
             callStubFn =
                     channel -> JetToCppGrpc.newStub(channel)::streamingCall;

        ServiceFactory<?,
               ? extends GrpcService<InputMessage, OutputMessage>> cppService =
               GrpcServices.bidirectionalStreamingService(
                       () -> MyUtils.getManagedChannelBuilder(host),
                       callStubFn);

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(TestSources.items(datum))
        .mapUsingServiceAsync(cppService,
                (service, line) -> {
                    InputMessage request = InputMessage.newBuilder().addInputValue(line).build();
                    LOGGER.info("Sending batch of {}", request.getInputValueCount());
                    return service.call(request).thenApply(result -> result.getOutputValue(0));
        })
        .setLocalParallelism(1)
        .writeTo(Sinks.logger());

        return pipeline;
    }
}
