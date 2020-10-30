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

package com.hazelcast.platform.demos.telco.churn;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.jet.python.PythonTransforms;

/**
 * XXX
 */
public class MLChurnDetector extends MyJobWrapper {
    protected static final String PYTHON_HANDLER_FN = "assess";
    protected static final String PYTHON_MODULE = "trainedmodel";
    protected static final String PYTHON_SUBDIR = "python";

    private static final Logger LOGGER = LoggerFactory.getLogger(MLChurnDetector.class);

    MLChurnDetector(long timestamp) {
        super(timestamp);
    }

    @Override
    JobConfig getJobConfig() {
        JobConfig jobConfig = super.getJobConfig();

        /*XXX
        jobConfig.addClass(TestSources.class);
        jobConfig.addClass(com.hazelcast.jet.pipeline.test.SimpleEvent.class);
        */

        return jobConfig;
    }

    public Pipeline getPipeline() {
        Pipeline pipeline = Pipeline.create();

        pipeline
        //XXX.readFrom(TestSources.itemStream(1)).withoutTimestamps()
        .readFrom(TestSources.items(List.of("hello", "world")))
        //.map(event -> event.getClass().toString())
        //XXX .apply(PythonTransforms.mapUsingPython(getPythonServiceConfig(PYTHON_MODULE)))
        .apply(PythonTransforms.mapUsingPythonBatch(getPythonServiceConfig(PYTHON_MODULE)))
        .writeTo(Sinks.logger());

        return pipeline;
    }

    /**
     * XXX
     * @param name
     * @return
     */
    protected static PythonServiceConfig getPythonServiceConfig(String name) {
        try {
            File temporaryDir = MLChurnDetector.getTemporaryDir(name, PYTHON_SUBDIR);

            PythonServiceConfig pythonServiceConfig = new PythonServiceConfig();
            pythonServiceConfig.setBaseDir(temporaryDir.toString());
            pythonServiceConfig.setHandlerFunction(PYTHON_HANDLER_FN);
            pythonServiceConfig.setHandlerModule(name);

            LOGGER.debug("Python module '{}{}.py', calling function '{}()'",
                "classpath:src/main/resources" + File.separator + PYTHON_SUBDIR + File.separator,
                pythonServiceConfig.handlerModule(),
                pythonServiceConfig.handlerFunction());

            return pythonServiceConfig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Python files are in "/src/main/resources" and hence in the classpath,
     * for easy deployment as a Docker image. Copy these to the main filesystem
     * to make it easier for the Python service to find them and stream them
     * to the cluster.
     * <p>
     *
     * @param name The job name, eg. "{@code trainedmodel}", also Python module prefix
     * @param sourcedirectory Within "{@code src/main/resources}" asubdirectory.
     * @return A folder containing the Python code copied from the classpath.
     */
    protected static File getTemporaryDir(String name, String sourceDirectory) throws Exception {

        Path targetDirectory = Files.createTempDirectory(name);
        targetDirectory.toFile().deleteOnExit();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String[] resourcesToCopy = { name + ".py", "requirements.txt" };
        for (String resourceToCopy : resourcesToCopy) {
            String relativeResourceToCopy = sourceDirectory + File.separator + resourceToCopy;
            try (InputStream inputStream = classLoader.getResourceAsStream(relativeResourceToCopy)) {
                if (inputStream == null) {
                    System.out.println(relativeResourceToCopy + ": not found in Jar's src/main/resources");
                } else {
                    LOGGER.trace("{}", relativeResourceToCopy);
                    Path targetFile = Paths.get(targetDirectory + File.separator + resourceToCopy);
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return targetDirectory.toFile();
    }
}
