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

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.test.AssertionSinks;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.jet.python.PythonServiceConfig;
import com.hazelcast.jet.python.PythonTransforms;

/**
 * <p>Test the correctness of "pi2.py" for various inputs
 * <p>
 */
public class Pi2IT extends AbstractJetIT {

    private static PythonServiceConfig pythonServiceConfig;

    @BeforeAll
    public static void beforeAll2() throws Exception {
        pythonServiceConfig = MyUtils.getPythonServiceConfig("pi2");
    }

    /**
     * <p>The origin "{@code (0,0)}" is inside the circle.
     * 100% of the input batch lies within the circle. so the
     * "{@code Pi == 4 * inside / total" should result true.
     * </p>
     */
    @Test
    public void originInsideCircle(TestInfo testInfo) throws Exception {
        double x = 0.0d;
        double y = 0.0d;
        String inside = "True";

        List<String> input = List.of(x + "," + y);
        List<String> expected = List.of(inside);

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(TestSources.items(input))
        .apply(PythonTransforms.mapUsingPythonBatch(pythonServiceConfig)).setLocalParallelism(1)
        .writeTo(AssertionSinks.assertOrdered(expected));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(testInfo.getDisplayName());

        hazelcastInstance.getJet().newJob(pipeline, jobConfig).join();
    }

    /**
     * <p>The apex "{@code (1,1)}" of the square is outside the
     * circle. 0% of the input batch lies within the circle. so the
     * "{@code Pi == 4 * inside / total" should result in false.
     * </p>
     */
    @Test
    public void apexOutsideCircle(TestInfo testInfo) throws Exception {
        double x = 1.0d;
        double y = 1.0d;
        String inside = "False";

        List<String> input = List.of(x + "," + y);
        List<String> expected = List.of(inside);

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(TestSources.items(input))
        .apply(PythonTransforms.mapUsingPythonBatch(pythonServiceConfig)).setLocalParallelism(1)
        .writeTo(AssertionSinks.assertOrdered(expected));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(testInfo.getDisplayName());

        hazelcastInstance.getJet().newJob(pipeline, jobConfig).join();
    }

    /**
     * <p>Pass a batch of three points.</p>
     * <p>The first is inside, the second is outside, the third is inside.
     * No rolling count is maintained, each should be evaluated individually.
     * </p>
     */
    @Test
    public void piIsRefinedGradually(TestInfo testInfo) throws Exception {
        double x1 = 0.0d;
        double y1 = 0.0d;
        double x2 = 1.0d;
        double y2 = 1.0d;
        double x3 = 0.1d;
        double y3 = 0.1d;
        String inside1 = "True";
        String inside2 = "False";
        String inside3 = "True";

        List<String> input = List.of(x1 + "," + y1, x2 + "," + y2, x3 + "," + y3);
        List<String> expected = List.of(inside1, inside2, inside3);

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(TestSources.items(input))
        .apply(PythonTransforms.mapUsingPythonBatch(pythonServiceConfig)).setLocalParallelism(1)
        .writeTo(AssertionSinks.assertOrdered(expected));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(testInfo.getDisplayName());

        hazelcastInstance.getJet().newJob(pipeline, jobConfig).join();
    }

}
