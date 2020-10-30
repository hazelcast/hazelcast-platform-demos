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

//FIXME import java.util.List;

//FIXME import org.junit.BeforeClass;
import org.junit.Test;

import com.hazelcast.jet.config.JobConfig;
//FIXME import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.Pipeline;
//FIXME import com.hazelcast.jet.pipeline.Sinks;
//FIXME import com.hazelcast.jet.pipeline.test.AssertionSinks;
//FIXME import com.hazelcast.jet.pipeline.test.TestSources;
//FIXME ?
//FIXME import com.hazelcast.jet.python.PythonServiceConfig;
//FIXME ?
//FIXME import com.hazelcast.jet.python.PythonTransforms;

/**
 * <p>Test the correctness of "trainedmodel.py" for various inputs
 * <p>
 */
public class MyChurnDetectorIT extends AbstractJetIT {

    /*XXXprivate static PythonServiceConfig pythonServiceConfig;

    @BeforeClass
    public static void beforeClass2() throws Exception {
        pythonServiceConfig =
            MLChurnDetector.getPythonServiceConfig(MLChurnDetector.PYTHON_MODULE);
    }*/

    /**
     * FIXME failsafe test, no classpath issue
     * XXX
     */
    @Test
    public void helloWorld() throws Exception {
        //List<String> input = List.of("hello", "world");
        //List<String> expected = List.of("5", "5");

        Pipeline pipeline = null;
        /*FIXME Pipeline.create();

        BatchStage<String> output =
                pipeline
                .readFrom(TestSources.items(input))
                .apply(PythonTransforms.mapUsingPythonBatch(pythonServiceConfig)).setLocalParallelism(1)
                ;

        output.writeTo(Sinks.logger());
        output.writeTo(AssertionSinks.assertOrdered(expected));
        */

        MLChurnDetector mlChurnDetector = new MLChurnDetector(System.currentTimeMillis());

        //JobConfig jobConfig = new JobConfig();
        //jobConfig.setName(super.testName.getMethodName());
        JobConfig jobConfig = mlChurnDetector.getJobConfig();
        pipeline = mlChurnDetector.getPipeline();

        jetInstance.newJob(pipeline, jobConfig).join();
    }
}
