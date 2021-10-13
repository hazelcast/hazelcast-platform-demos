/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.ml.ri;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import com.hazelcast.jet.datamodel.WindowResult;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.topic.ITopic;

/**
 * <p>Utility functions to share between {@link Pi1Job} and {@link Pi2Job}.
 * </p>
 */
public class MyUtils {

    protected static final WindowDefinition FIVE_SECOND_WINDOW
        = WindowDefinition.tumbling(TimeUnit.SECONDS.toMillis(5));

    /**
     * <p>Configuration for the Python runner. Where to find the Python module,
     * which is presumed to have a "{@code handle()}" function.
     *
     * @param name The job name, eg. "{@code pi1}", used as a folder prefix
     * @return Python configuration for use in a Jet job.
     */
    protected static PythonServiceConfig getPythonServiceConfig(String name) throws Exception {
        File temporaryDir = MyUtils.getTemporaryDir(name);

        PythonServiceConfig pythonServiceConfig = new PythonServiceConfig();
        pythonServiceConfig.setBaseDir(temporaryDir.toString());
        pythonServiceConfig.setHandlerFunction("handle");
        pythonServiceConfig.setHandlerModule(name);

        return pythonServiceConfig;
    }

    /**
     * <p>Publish the event to a topic for all listeners to be aware
     * of.
     * </p>
     *
     * @param klass - Sending job class name.
     * @param topicName - Topic name, should always be "{@code pi}".
     */
    protected static Sink<? super WindowResult<?>> buildTopicSink(Class<?> klass, String topicName) {
        return SinkBuilder.sinkBuilder(
                        "topicSink-" + topicName,
                        context -> context.hazelcastInstance().getTopic(topicName)
                        )
                        .receiveFn((ITopic<Object> iTopic, WindowResult<?> item) ->
                            iTopic.publish(klass.getSimpleName() + "," + item.result().toString()))
                        .build();
    }

    /**
     * <p>Python files are in "/src/main/resources" and hence in the classpath,
     * for easy deployment as a Docker image. Copy these to the main filesystem
     * to make it easier for the Python service to find them and stream them
     * to the cluster.
     * <p>
     *
     * @param name The job name, eg. "{@code pi1}", used as a folder prefix
     * @return A folder containing the Python code copied from the classpath.
     */
    protected static File getTemporaryDir(String name) throws Exception {

        Path targetDirectory = Files.createTempDirectory(name);
        targetDirectory.toFile().deleteOnExit();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String[] resourcesToCopy = { name + ".py", "requirements.txt" };
        for (String resourceToCopy : resourcesToCopy) {
            try (InputStream inputStream = classLoader.getResourceAsStream(resourceToCopy)) {
                if (inputStream == null) {
                    System.out.println(resourceToCopy + ": not found in Jar's src/main/resources");
                } else {
                    Path targetFile = Paths.get(targetDirectory + File.separator + resourceToCopy);
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return targetDirectory.toFile();
    }

}
