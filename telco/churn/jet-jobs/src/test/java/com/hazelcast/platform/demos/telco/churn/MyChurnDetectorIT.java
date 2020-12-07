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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import com.hazelcast.collection.IList;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.jet.python.PythonTransforms;

/**
 * <p>Test the correctness of "trainedmodel.py" for various inputs
 * <p>
 */
public class MyChurnDetectorIT extends AbstractJetIT {

    private static PythonServiceConfig pythonServiceConfig;
    private static final String SINK_ILIST_NAME = "junit";

    @BeforeAll
    public static void beforeAll2() throws Exception {
        pythonServiceConfig =
            MLChurnDetector.getPythonServiceConfig(MLChurnDetector.PYTHON_MODULE);
    }

    @BeforeEach
    void setUp() throws Exception {
        jetInstance.getList(SINK_ILIST_NAME).clear();
    }

    /**
     * <p>Pass a record to Python to evaluate.
     * </p>
     */
    @DisplayName("call Python")
    @Test
    public void passRecordToPython(TestInfo testInfo) throws Exception {
        String key = "(123)-456-7890";
        String callDataRecordDropped = ",,,,true,,,,,,,";
        String callDataRecordNotDropped = ",,,,false,,,,,,,";
        String customer = ",,,,,,,";
        String sentimentNotExisting = ",,";
        String sentimentExisting = "5.5,2.2,";

        String inputDroppedWSentiment = key + "," + callDataRecordDropped
                + "," + customer + "," + sentimentExisting;
        String inputDroppedWoSentiment = key + "," + callDataRecordDropped
                + "," + customer + "," + sentimentNotExisting;
        String inputNotDroppedWSentiment = key + "," + callDataRecordNotDropped
                + "," + customer + "," + sentimentExisting;
        String inputNotDroppedWoSentiment = key + "," + callDataRecordNotDropped
                + "," + customer + "," + sentimentNotExisting;
        List<String> input =
                List.of(inputDroppedWSentiment, inputDroppedWoSentiment,
                        inputNotDroppedWSentiment, inputNotDroppedWoSentiment);

        List<Double> expectedCurrent = List.of(11.9D, 3.1D, 5.5D, 0.0D);
        List<Double> expectedPrevious = List.of(5.5D, 0.0D, 5.5D, 0.0D);

        Pipeline pipeline = Pipeline.create();
        pipeline
        .readFrom(TestSources.items(input))
        .apply(PythonTransforms.mapUsingPythonBatch(pythonServiceConfig)).setLocalParallelism(1)
        .writeTo(Sinks.list(SINK_ILIST_NAME));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(testInfo.getDisplayName());
        jetInstance.newJob(pipeline, jobConfig).join();

        IList<String> iList = jetInstance.getList(SINK_ILIST_NAME);
        List<String> list = iList.stream().collect(Collectors.toList());

        assertThat(list).size().isEqualTo(expectedCurrent.size());
        assertThat(list).size().isEqualTo(expectedPrevious.size());

        for (int i = 0 ; i < expectedCurrent.size(); i++) {
            String[] tokens = list.get(i).split(",");
            Double newCurrent = Double.valueOf(tokens[tokens.length - 2]);
            Double newPrevious = Double.valueOf(tokens[tokens.length - 1]);

            // Exact match on previous, allow variance for current
            assertThat(newPrevious).isEqualTo(expectedPrevious.get(i));
            // old_annoyance = current_pct - previous_pct
            // new_annoyance = float(3.1) + old_annoyance + current_pct + random.random()
            assertThat(newCurrent).isGreaterThanOrEqualTo(expectedCurrent.get(i));
            assertThat(newCurrent).isLessThan(expectedCurrent.get(i) + 1.0d);
            System.out.println(expectedCurrent.get(i) + " new Current " + newCurrent);
        }
    }

}
