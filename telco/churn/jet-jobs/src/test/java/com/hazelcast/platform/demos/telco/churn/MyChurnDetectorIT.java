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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.platform.demos.telco.churn.domain.Sentiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.AssertionSinks;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.jet.python.PythonTransforms;

/**
 * <p>Test the correctness of "trainedmodel.py" for various inputs
 * <p>
 */
public class MyChurnDetectorIT extends AbstractJetIT {

    private static PythonServiceConfig pythonServiceConfig;
    private static long nowMS;

    @BeforeAll
    public static void beforeAll2() throws Exception {
        pythonServiceConfig =
            MLChurnDetector.getPythonServiceConfig(MLChurnDetector.PYTHON_MODULE);

        nowMS = System.currentTimeMillis();
    }

    @BeforeEach
    void setUp() throws Exception {
    }

    /**
     * <p>Sentiment may be initially null</p>
     */
    @DisplayName("format for Python with nulls")
    @Test
    //FIXME
    @Disabled("FIXME")
    public void formatForPythonWithNulls(TestInfo testInfo) {
        String callerTelno = testInfo.getDisplayName();
        HazelcastJsonValue cdr = makeCDR(callerTelno);
        HazelcastJsonValue customer = makeCustomer(callerTelno);
        Sentiment sentiment = null;

        Tuple4<String, HazelcastJsonValue, HazelcastJsonValue, Sentiment> tuple4
                = Tuple4.tuple4(callerTelno, cdr, customer, sentiment);
        List<Tuple4<String, HazelcastJsonValue, HazelcastJsonValue, Sentiment>> input =
                List.of(tuple4);
        List<String> expected = List.of(",,,");

        Pipeline pipeline = Pipeline.create();

        BatchStage<String> output =
                pipeline
                        .readFrom(TestSources.items(input))
                        .map(MLChurnDetector.formatForPython())
                ;

        output.writeTo(Sinks.logger());
        output.writeTo(AssertionSinks.assertOrdered(expected));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(testInfo.getDisplayName());

        jetInstance.newJob(pipeline, jobConfig).join();
    }

    /**
     * <p>Sentiment will ideally not be null</p>
     */
    @DisplayName("format for Python without nulls")
    @Test
    //FIXME
    @Disabled("FIXME")
    public void formatForPythonWithoutNulls(TestInfo testInfo) {
        String callerTelno = testInfo.getDisplayName();
        HazelcastJsonValue cdr = makeCDR(callerTelno);
        HazelcastJsonValue customer = makeCustomer(callerTelno);
        Sentiment sentiment = new Sentiment();
        sentiment.setCurrent(1.0d);
        sentiment.setPrevious(0.9d);
        sentiment.setUpdated(LocalDate.now().atStartOfDay());

        Tuple4<String, HazelcastJsonValue, HazelcastJsonValue, Sentiment> tuple4
                = Tuple4.tuple4(callerTelno, cdr, customer, sentiment);
        List<Tuple4<String, HazelcastJsonValue, HazelcastJsonValue, Sentiment>> input =
                List.of(tuple4);
        List<String> expected = List.of(",,,");

        Pipeline pipeline = Pipeline.create();

        BatchStage<String> output =
                pipeline
                        .readFrom(TestSources.items(input))
                        .map(MLChurnDetector.formatForPython())
                ;

        output.writeTo(Sinks.logger());
        output.writeTo(AssertionSinks.assertOrdered(expected));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(testInfo.getDisplayName());

        jetInstance.newJob(pipeline, jobConfig).join();
    }

    /**
     * <p>At this point, the Python module is just counting characters
     * in words.
     * </p>
     */
    @DisplayName("call Python")
    @Test
    //FIXME
    @Disabled("FIXME")
    public void helloWorld(TestInfo testInfo) throws Exception {
        List<String> input = List.of("Hello", "World!");
        List<String> expected = List.of("5", "6");

        Pipeline pipeline = Pipeline.create();

        BatchStage<String> output =
                pipeline
                .readFrom(TestSources.items(input))
                .apply(PythonTransforms.mapUsingPythonBatch(pythonServiceConfig)).setLocalParallelism(1)
                ;

        output.writeTo(Sinks.logger());
        output.writeTo(AssertionSinks.assertOrdered(expected));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(testInfo.getDisplayName());

        jetInstance.newJob(pipeline, jobConfig).join();
    }

    /**
     * <p>Helper to make call data record</p>
     */
    private HazelcastJsonValue makeCDR(String callerTelno) {
        StringBuilder stringBuilder = new StringBuilder("{ ");
        stringBuilder.append("\"id\" : \"").append(UUID.randomUUID()).append("\"");
        stringBuilder.append(",\"callerTelno\" : \"").append(callerTelno).append("\"");
        stringBuilder.append(",\"callerMastId\" : \"").append("callerMastId".toUpperCase()).append("\"");
        stringBuilder.append(",\"calleeTelno\" : \"").append("calleeTelno".toUpperCase()).append("\"");
        stringBuilder.append(",\"calleeMastId\" : \"").append("calleeMastId".toUpperCase()).append("\"");
        stringBuilder.append(",\"startTimestamp\" : ").append(nowMS);
        stringBuilder.append(",\"durationSeconds\" : ").append(0);
        stringBuilder.append(",\"callSuccessful\" : ").append(false);
        stringBuilder.append(",\"createdBy\" : \"").append("createdBy".toUpperCase()).append("\"");
        stringBuilder.append(",\"createdDate\" : ").append(nowMS);
        stringBuilder.append(",\"lastModifiedBy\" : \"").append("lastModifiedBy".toUpperCase()).append("\"");
        stringBuilder.append(",\"lastModifiedDate\" : ").append(nowMS);
        stringBuilder.append(" }");
        return new HazelcastJsonValue(stringBuilder.toString());
    }

    /**
     * <p>Helper to make customer</p>
     */
    private HazelcastJsonValue makeCustomer(String callerTelno) {
        StringBuilder stringBuilder = new StringBuilder("{ ");
        stringBuilder.append("\"id\" : \"").append(callerTelno).append("\"");
        stringBuilder.append(",\"firstName\" : \"").append("firstName".toUpperCase()).append("\"");
        stringBuilder.append(",\"lastName\" : \"").append("lastName".toUpperCase()).append("\"");
        stringBuilder.append(",\"accountType\" : \"").append("accountType".toUpperCase()).append("\"");
        stringBuilder.append(",\"createdBy\" : \"").append("createdBy".toUpperCase()).append("\"");
        stringBuilder.append(",\"createdDate\" : ").append(nowMS);
        stringBuilder.append(",\"lastModifiedBy\" : \"").append("lastModifiedBy".toUpperCase()).append("\"");
        stringBuilder.append(",\"lastModifiedDate\" : ").append(nowMS);
        stringBuilder.append(" }");
        return new HazelcastJsonValue(stringBuilder.toString());
    }
}
