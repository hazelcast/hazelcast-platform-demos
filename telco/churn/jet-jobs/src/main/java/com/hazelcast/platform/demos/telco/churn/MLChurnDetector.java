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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.JournalInitialPosition;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.jet.python.PythonTransforms;
import com.hazelcast.platform.demos.telco.churn.domain.CallDataRecordKey;
import com.hazelcast.platform.demos.telco.churn.domain.Sentiment;

/**
 * <p>Pass changes to call data records to sentiment analysis.
 * </p>
 * <pre>
 *                +------( 1 )------+
 *                | Journal Source  |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 2 )------+
 *                |     Filter      |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 3 )------+
 *                | Enrich via map 1|
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 4 )------+
 *                | Enrich via map 2|
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 5 )------+
 *                |   Re-format     |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 6 )------+
 *                |     Python      |
 *                +-----------------+
 * XXX
 * </pre>
 * <p>
 * The steps:
 * </p>
 * <ol>
 * <li>
 * <p>
 * Journal source
 * </p>
 * <p>The history of changes to the "{@code cdr}" map. If an entry is changed
 * twice, we get both changes, not just the current value.
 * </p>
 * </li>
 * </ol>
 * <li>
 * <p>
 * Filter
 * </p>
 * <p>Exclude changes made at Hazelcast or legacy, we only wish changes
 * from the data feed.
 * </p>
 * </li>
 * <li>
 * <p>
 * Enrich from map, 1.
 * </p>
 * <p>Look up the "{@code customer}" map for the customer record
 * corresponding to the call's customer.
 * </p>
 * </li>
 * <li>
 * <p>
 * Enrich from map, 2.
 * </p>
 * <p>Look up the "{@code sentiment}" map for the sentiment record
 * corresponding to the call's customer.
 * </p>
 * </li>
 * <li>
 * <p>
 * Reformat for Python
 * </p>
 * <p>Convert a "{@link Tuple4}" of the call key, value, customer and
 * sentiment to pass into Python.
 * </p>
 * </li>
 * <li>
 * <p>
 * Python
 * </p>
 * <p>Invoke the Python sentiment analysis module.
 * </p>
 * </li>
 * <li>
 * <p>
 * XXX source
 * </p>
 * <p>XXX
 * </p>
 * </li>
 */
public class MLChurnDetector extends MyJobWrapper {
    protected static final String PYTHON_HANDLER_FN = "assess";
    protected static final String PYTHON_MODULE = "trainedmodel";
    protected static final String PYTHON_SUBDIR = "python";

    private static final Logger LOGGER = LoggerFactory.getLogger(MLChurnDetector.class);

    MLChurnDetector(long timestamp) {
        super(timestamp);
    }

    /**
     * <p>Create a processing pipeline as described above.
     * Use other methods for the complex bits rather than inline.
     * </p>
     */
    public Pipeline getPipeline() {
        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(Sources.<CallDataRecordKey, HazelcastJsonValue>mapJournal(MyConstants.IMAP_NAME_CDR,
                JournalInitialPosition.START_FROM_OLDEST)).withoutTimestamps()
        .filter(entry -> {
            JSONObject jsonObject = new JSONObject(entry.getValue().toString());
            return ("churn-data-feed".equals(jsonObject.getString("lastModifiedBy")));
        }).setName("data-feed only")
        .map(entry -> Tuple2.tuple2(entry.getKey().getPartitionKey(), entry.getValue()))
            .setName("reformat key")
        .mapUsingIMap(MyConstants.IMAP_NAME_CUSTOMER,
                Tuple2::f0,
                (tuple2, customer) ->
                    Tuple3.tuple3(tuple2.f0(), tuple2.f1(), (HazelcastJsonValue) customer))
            .setName("enrich with customer")
        .mapUsingIMap(MyConstants.IMAP_NAME_SENTIMENT,
                Tuple3::f0,
                (tuple3, sentiment) ->
                    Tuple4.tuple4(tuple3.f0(), tuple3.f1(), tuple3.f2(),
                            (Sentiment) sentiment))
            .setName("enrich with sentiment")
        .map(MLChurnDetector.formatForPython())
            .setName("reformat for Python")
        .apply(PythonTransforms.mapUsingPython(getPythonServiceConfig(PYTHON_MODULE)))
            .setName(PYTHON_MODULE)
        .map(str -> {
            //XXX Debug
            System.out.println("@@@5 " + str);
            return null;
        })
        .writeTo(Sinks.logger());

        return pipeline;
    }


    /**
     * <p>Reformat a four-tuple of customer id, call data record, customer and
     * sentiment into CSV for simpler processing by Python.
     * </p>
     *
     * @return A string, that is in CSV format with preset fields
     */
    protected static FunctionEx
        <Tuple4<String, HazelcastJsonValue, HazelcastJsonValue, Sentiment>, String>
        formatForPython() {
        return tuple4 -> {
            StringBuilder stringBuilder = new StringBuilder();

            // Telno
            stringBuilder.append(tuple4.f0());
            stringBuilder.append(",");

            // CDR
            //XXX stringBuilder.append(csvFromCallDataRecord(tuple4.f1()));
            stringBuilder.append(",");

            // Customer
            //XXX stringBuilder.append(csvFromCustomer(tuple4.f2()));
            stringBuilder.append(",");

            // Sentiment
            stringBuilder.append(MyCsvUtils.toCSVSentiment(tuple4.f3()));

            return stringBuilder.toString();
        };
    }

    /**
     * <p>Configuration to supply a Python module from the classpath.
     * </p>
     *
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
