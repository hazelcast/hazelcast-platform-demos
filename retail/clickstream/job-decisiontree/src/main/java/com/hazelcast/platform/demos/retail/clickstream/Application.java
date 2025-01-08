/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.retail.clickstream;

import java.io.InputStream;
import java.util.Properties;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.platform.demos.retail.clickstream.job.DecisionTreePrediction;

/**
 * <p>
 * Entry point. Hazelcast instance is provided by job submission CLI.
 * </p>
 * <p>TODO: Coding is same for {@code DecisionTree} and {@code Gaussian}
 * submission.
 * </p>
 */
public class Application {

    public static void main(String[] args) throws Exception {
        String filename = "application.properties";

        ClassLoader classLoader = Application.class.getClassLoader();

        Properties properties = new Properties();
        try (InputStream inputStream = classLoader.getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new RuntimeException(filename + ": not found in Jar");
            } else {
                properties.load(inputStream);
            }
        }
        properties.list(System.out);

        HazelcastInstance hazelcastInstance = Hazelcast.bootstrappedInstance();

        String graphiteHost = hazelcastInstance
                .getMap(MyConstants.IMAP_NAME_CONFIG)
                .get(MyConstants.CONFIG_MAP_KEY_GRAPHITE).toString();

        String jobName = DecisionTreePrediction.class.getSimpleName() + "@" + properties.getProperty("my.build-timestamp", "");
        if (jobName.endsWith("Z")) {
            jobName = jobName.substring(0, jobName.length() - 1);
        }
        System.out.println("~~~");
        System.out.println("jobName == '" + jobName + "'");
        System.out.println("~~~");

        Pipeline pipelineDecisionTreePrediction = DecisionTreePrediction.buildPipeline(graphiteHost, classLoader);

        JobConfig jobConfigDecisionTreePrediction = new JobConfig();
        jobConfigDecisionTreePrediction.addClass(DecisionTreePrediction.class);
        jobConfigDecisionTreePrediction.addClass(MyUtils.class);
        jobConfigDecisionTreePrediction.setName(jobName);

        // Fails if job exists with same job name, unlike "newJobIfAbsent"
        hazelcastInstance.getJet().newJob(pipelineDecisionTreePrediction, jobConfigDecisionTreePrediction);

        System.exit(0);
    }

}
