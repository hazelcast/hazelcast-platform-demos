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

package com.hazelcast.jet.python;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Queue;

import com.hazelcast.jet.JetException;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.jet.grpc.impl.GrpcUtil;
import com.hazelcast.jet.python.impl.grpc.InputMessage;
import com.hazelcast.jet.python.impl.grpc.InputMessage.Builder;
import com.hazelcast.jet.python.impl.grpc.JetToPythonGrpc;
import com.hazelcast.jet.python.impl.grpc.JetToPythonGrpc.JetToPythonStub;
import com.hazelcast.jet.python.impl.grpc.OutputMessage;
import com.hazelcast.logging.ILogger;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>{@link JetToPythonServer} is not public, so this class
 * needs to be in package {@code com.hazelcast.jet.python}
 * </p>
 * <p>{@link DagPythonServiceContext} is a clone of {@link PythonServiceContext}
 * without the assumption the working direction has been attached to the
 * processor context by Jet.
 * </p>
 * <p>{@link DagJetToPythonServer} is a clone of {@link JetToPythonService},
 * as this would otherwise make a static reference to {@link PythonServiceContext}
 * not {@link DagPythonServiceContext}.
 * </p>
 */
@Slf4j
public class DagPythonProcessor extends AbstractProcessor {
    private static final int CREATE_CONTEXT_RETRY_COUNT = 2;
    private static final int CREATE_CONTEXT_RETRY_SLEEP_TIME_MILLIS = 10_000;

    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final Queue<CompletableFuture<List<String>>> futureQueue = new ConcurrentLinkedQueue<>();
    private final ILogger iLogger;
    private final DagJetToPythonServer jetToPythonServer;
    private final ManagedChannel managedChannel;
    private final String moduleName;
    private final List<String> module;
    private final String methodName;
    private final List<String> requirements;
    private final StreamObserver<InputMessage> streamObserver;
    private volatile Throwable exceptionInOutputObserver;

    public DagPythonProcessor(ProcessorSupplier.Context arg0, String arg1, List<String> arg2, String arg3, List<String> arg4)
            throws Exception {
        this.iLogger = arg0.logger();
        this.moduleName = arg1;
        this.module = arg2;
        this.methodName = arg3;
        this.requirements = arg4;
        log.trace("DagPythonProcessor('{}', '{}', '{}', '{}')", MyUtils.truncateToString(this.moduleName),
                MyUtils.truncateToString(this.module), MyUtils.truncateToString(this.methodName),
                MyUtils.truncateToString(this.requirements));

        Path targetDirectory = this.buildFiles();

        DagPythonServiceContext pythonServiceContext = this.buildPythonServiceContext(arg0, targetDirectory);
        this.jetToPythonServer = new DagJetToPythonServer(pythonServiceContext.runtimeBaseDir(), arg0.logger());
        try {
            int serverPort = this.jetToPythonServer.start();
            this.managedChannel = NettyChannelBuilder.forAddress("127.0.0.1", serverPort)
                                      .usePlaintext()
                                      .build();
            JetToPythonStub client = JetToPythonGrpc.newStub(this.managedChannel);
            this.streamObserver = client.streamingCall(new OutputMessageObserver());
        } catch (Throwable e) {
            this.jetToPythonServer.stop();
            throw new JetException(DagPythonProcessor.class.getSimpleName(), e);
        }
    }

    /**
     * <p>Unpack the named module and requirements locally.
     * </p>
     *
     * @return
     * @throws Exception
     */
    private Path buildFiles() throws Exception {
        Path targetDirectory = Files.createTempDirectory(this.moduleName);

        targetDirectory.toFile().deleteOnExit();
        if (this.requirements.size() > 0) {
            this.writeFile(targetDirectory, "requirements.txt", this.requirements);
        }
        this.writeFile(targetDirectory, this.moduleName + ".py", this.module);

        return targetDirectory;
    }

    private Path writeFile(Path targetDirectory, String fileName, List<String> lines) throws Exception {
        log.trace("writeFile({} {})", fileName, lines.size());
        Path targetFile = Paths.get(targetDirectory + File.separator + fileName);
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(targetFile)) {
            while (lines.size() > 0) {
                bufferedWriter.write(lines.remove(0));
                bufferedWriter.write(System.lineSeparator());
            }
        }
        return targetFile;
    }

    private DagPythonServiceContext buildPythonServiceContext(ProcessorSupplier.Context context, Path targetDirectory) {
        PythonServiceConfig pythonServiceConfig = this.buildPythonServiceConfig(targetDirectory);
        JetException jetException = null;
        for (int i = CREATE_CONTEXT_RETRY_COUNT; i >= 0 ; i--) {
            try {
                return new DagPythonServiceContext(context, pythonServiceConfig);
            } catch (JetException exception) {
                jetException = exception;
                context.logger().warning(
                        "PythonService context creation failed, " + (i > 0 ? "will retry" : "giving up"),
                        exception);
                try {
                    Thread.sleep(CREATE_CONTEXT_RETRY_SLEEP_TIME_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new JetException(e);
                }
            }
        }
        throw jetException;
    }

    private PythonServiceConfig buildPythonServiceConfig(Path targetDirectory) {
        PythonServiceConfig pythonServiceConfig = new PythonServiceConfig();
        pythonServiceConfig.setBaseDir(targetDirectory.toString());
        pythonServiceConfig.setHandlerFunction(this.methodName);
        pythonServiceConfig.setHandlerModule(this.moduleName);

        log.trace("Python module '{}{}.py', calling function '{}()'",
                pythonServiceConfig.baseDir().toString() + File.separator,
                pythonServiceConfig.handlerModule(),
                pythonServiceConfig.handlerFunction());

        return pythonServiceConfig;
    }

    /**
     * <p>Call Python</p>
     */
    @Override
    protected boolean tryProcess(int ordinal, Object item) {
        checkForServerError();
        Builder requestBuilder = InputMessage.newBuilder();
        requestBuilder.addInputValue(item.toString());
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        futureQueue.add(future);
        this.streamObserver.onNext(requestBuilder.build());
        try {
            List<String> l = future.get();
            if (l.size() == 1) {
                String s = l.get(0);
                super.tryEmit(s);
            } else {
                log.error("tryProcess({}, '{}') -> Python returned {} items", ordinal, item, l.size());
            }
        } catch (Exception e) {
            String message = String.format("tryProcess(%d, '%s')", ordinal, item);
            log.error(message, e);
        }
        return true;
    }

    private void checkForServerError() {
        if (completionLatch.getCount() == 0) {
            throw new JetException("PythonService broke down: " + exceptionInOutputObserver, exceptionInOutputObserver);
        }
    }

    @Override
    public void close() throws Exception {
        log.trace("close()");
        boolean interrupted = Thread.interrupted();
        try {
            this.streamObserver.onCompleted();
            if (!this.completionLatch.await(1, TimeUnit.SECONDS)) {
                log.info("gRPC call has not completed on time");
            }
            GrpcUtil.shutdownChannel(this.managedChannel, this.iLogger, 1);
            this.jetToPythonServer.stop();
        } catch (Exception e) {
            throw new JetException("PythonService.destroy() failed: " + e, e);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * <p>Captures messages from Python.
     * </p>
     */
    private class OutputMessageObserver implements StreamObserver<OutputMessage> {
        @Override
        public void onNext(OutputMessage outputItem) {
            try {
                futureQueue.remove().complete(outputItem.getOutputValueList());
            } catch (Throwable e) {
                log.error("onNext()", e);
                exceptionInOutputObserver = e;
                completionLatch.countDown();
            }
        }

        @Override
        public void onError(Throwable e) {
            try {
                e = GrpcUtil.translateGrpcException(e);

                exceptionInOutputObserver = e;
                CompletableFuture<List<String>> future;
                while ((future = futureQueue.poll()) != null) {
                    future.completeExceptionally(e);
                }
            } finally {
                completionLatch.countDown();
            }
        }

        @Override
        public void onCompleted() {
            CompletableFuture<List<String>> future;
            while ((future = futureQueue.poll()) != null) {
                future.completeExceptionally(new JetException("Completion signaled before the future was completed"));
            }
            completionLatch.countDown();
        }
    }
}
