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
import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

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
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamStage;
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
 *                |   Re-format IN  |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 6 )------+
 *                |     Python      |
 *                +-----------------+
 *                         |
 *                         |
 *                         |
 *                +------( 7 )------+
 *                |   Re-format OUT |
 *                +-----------------+
 *                    /         \
 *                   /           \
 *                  /             \
 *    +------( 8 )------+     +------( 9 )------+
 *    |     Filter      |     |  Save to IMap   |
 *    +-----------------+     +-----------------+
 *             |
 *             |
 *             |
 *    +------( 10)------+
 *    | Alert to Topic  |
 *    +-----------------+
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
 * Reformat from Python
 * </p>
 * <p>Turn the output from Python into a "{@link Sentiment}" map
 * entry.
 * </p>
 * </li>
 * <li>
 * <p>
 * Filter
 * </p>
 * <p>On the alerting leg of processing, discard "{@link Sentiment}"
 * records where the customer is not perceived as likely to churn.
 * </p>
 * </li>
 * <li>
 * <p>
 * Save to IMap
 * </p>
 * <p>Save the "{@link Sentiment}" record into an "{@link IMap}",
 * using a simple save to overwrite the content. We could use
 * a more clever "{@link EntryProcessor}" if we needed custom logic to
 * merge rather than replace.
 * </p>
 * <p>Note there is a race condition here. We read in step 4 from
 * a map and write to it in step 9. If there were two records in
 * a row, this would present a race. If the first is a dropped
 * call, duration 0 seconds, it is possible the caller would attempt
 * the call again immediately, so two records for the same {@link Sentiment}
 * key is possible.
 * </li>
 * <li>
 * <p>
 * Alert
 * </p>
 * <p>Produce an alert to a "{@link ITopic}", this customer is at
 * risk of churning.
 * </p>
 * </li>
 * </ol>
 */
public class MLChurnDetector extends MyJobWrapper {
    protected static final String PYTHON_HANDLER_FN = "assess";
    protected static final String PYTHON_MODULE = "trainedmodel";
    protected static final String PYTHON_SUBDIR = "python";
    protected static final int PYTHON_OUTPUT_COMMAS = 26;

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

        StreamStage<Entry<String, Sentiment>> sentimentStream = pipeline
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
        .map(MLChurnDetector::makeSentimentEntry)
            .setName("reformat for Sentiment");

        // Branch the sentiment stream, always saving, possibly alerting

        // Step 8 & 10 above
        sentimentStream
        .filter(entry -> {
            Sentiment sentiment = entry.getValue();
            // Crossed threshold
            return (sentiment.getPrevious() <= MyConstants.SENTIMENT_THESHOLD_FOR_ALERTING_75_PCT
                    && sentiment.getCurrent() > MyConstants.SENTIMENT_THESHOLD_FOR_ALERTING_75_PCT);
        }).setName("filter for at-risk-of-churn customers")
        .writeTo(MLChurnDetector.myTopicSink(MyConstants.ITOPIC_NAME_SLACK));

        // Step 9 above, but see comment on race condition
        sentimentStream.writeTo(Sinks.map(MyConstants.IMAP_NAME_SENTIMENT));

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
            stringBuilder.append(MyCsvUtils.toCSVCallDataRecord(tuple4.f1()));
            stringBuilder.append(",");

            // Customer
            stringBuilder.append(MyCsvUtils.toCSVCustomer(tuple4.f2()));
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
                    LOGGER.error(relativeResourceToCopy + ": not found in Jar's src/main/resources");
                } else {
                    LOGGER.trace("{}", relativeResourceToCopy);
                    Path targetFile = Paths.get(targetDirectory + File.separator + resourceToCopy);
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return targetDirectory.toFile();
    }

    /**
     * <p>Python works of a CSV line, the first of which is the
     * customer's key. It appends the derived current and previous
     * sentiment values. The output previous is the same as the
     * input current, so it is somewhere else in the output, but
     * easier to find if it's the last two values.
     * </p>
     *
     * @param csv From Python, first is key, last two are sentiment
     * @return
     */
    private static Entry<String, Sentiment> makeSentimentEntry(String csv) {
        String[] tokens = csv.split(",");
        if (tokens.length != PYTHON_OUTPUT_COMMAS) {
            LOGGER.error("CSV from Python has {} tokens, {}", tokens.length, csv);
            return null;
        }
        try {
            String key = tokens[0];
            Sentiment value = new Sentiment();
            double current = Double.valueOf(tokens[tokens.length - 2]);
            double previous = Double.valueOf(tokens[tokens.length - 1]);
            value.setUpdated(LocalDateTime.now());
            value.setCurrent(current);
            value.setPrevious(previous);
            LOGGER.debug("{} {}", key, value);
            return new SimpleImmutableEntry<String, Sentiment>(key, value);
        } catch (Exception e) {
            LOGGER.error(csv, e);
            return null;
        }
    }

    /**
     * <p>Create a simple sink, posts an entry to a topic.
     * </p>
     *
     * @param topicName will always be "{@code slack}".
     */
    private static Sink<Entry<String, Sentiment>> myTopicSink(String topicName) {
        return SinkBuilder.sinkBuilder(
                    "topicSink-" + topicName,
                    context -> context.jetInstance().getHazelcastInstance().<String>getTopic(topicName)
                )
                .<Entry<String, Sentiment>>receiveFn(
                        (iTopic, entry) -> {
                            String message =
                                    String.format("Churn risk, customer '%s', at %.0f%%",
                                            entry.getKey(), entry.getValue().getCurrent());
                            LOGGER.info("Publish on '{}': '{}' : {}",
                                    iTopic.getName(), message, entry.getValue());
                            iTopic.publish(message);
                        })
                .preferredLocalParallelism(1)
                .build();
    }
}
