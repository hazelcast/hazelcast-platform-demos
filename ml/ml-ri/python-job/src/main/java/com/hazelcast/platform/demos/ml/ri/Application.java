/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
public class Application {

    /**
     * <p>Start a Jet client, launch a job, then disconnect.
     * Use command line to determine which job of two to use.
     * </p>
     */
    public static void main(String[] args) {
        String pythonJob;
        if (args.length == 0) {
            pythonJob = "pi1";
            System.out.println("No args, pythonJob=='" + pythonJob + ".py'");
        } else {
            pythonJob = args[0].equals("pi1.py") ? "pi1" : "pi2";
            System.out.println("Arg='" + args[0] + "', pythonJob=='" + pythonJob + ".py'");
        }

        ClientConfig clientConfig = ApplicationConfig.buildClientConfig();

        HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

        try {
            Pipeline pipeline = null;
            JobConfig jobConfig = new JobConfig();
            if (pythonJob.equals("pi1")) {
                pipeline = Pi1Job.buildPipeline();
                jobConfig.setName(Pi1Job.class.getSimpleName());
                jobConfig.addClass(Pi1Job.class);
            } else {
                pipeline = Pi2Job.buildPipeline();
                jobConfig.setName(Pi2Job.class.getSimpleName());
                jobConfig.addClass(Pi2Job.class);
            }
            jobConfig.addClass(MyUtils.class);

            // Throws exception if job exists
            hazelcastInstance.getJet().newJob(pipeline, jobConfig);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        hazelcastInstance.shutdown();
    }

}
