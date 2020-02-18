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

package com.hazelcast.platform.demos.banking.trademonitor;

import com.hazelcast.jet.JetInstance;
/*XXX
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
*/

/**
 * <p>Initialise the Jet cluster to ensure the necessary extra parts
 * exist and necessary jobs are running. These are idempotent operations,
 * the will only do anything for the first node in the Jet cluster.
 * </p>
 */
public class ApplicationInitializer {

    /**
     * <p>Ensure the necessary {@link com.hazelcast.core.DistributedObject} exist to
     * hold processing results. Launch the Jet jobs for this example.
     * </p>
     */
    public static void initialise(JetInstance jetInstance) {
        createNeededObjects(jetInstance);
        launchNeededJobs(jetInstance);
    }


    /**
     * <p>Access the {@link com.hazelcast.map.IMap} and other objects
     * that are used by the example. This will create them on first
     * access, so ensuring all are visible from the outset.
     * </p>
     */
    static void createNeededObjects(JetInstance jetInstance) {
        jetInstance.getHazelcastInstance().getMap("trades");
    }


    /**
     * <p>An input source is needed for the example jobs
     * to process.
     * </p>
     * <p>Create one job per cluster that generates X &amp; Y
     * co-ordinates to the map "{@code points}".
     * </p>
     */
    static void launchNeededJobs(JetInstance jetInstance) {
        /*XXX
        Pipeline pipeline = RandomXYGenerator.buildPipeline();

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(RandomXYGenerator.class.getName());

        jetInstance.newJobIfAbsent(pipeline, jobConfig);
        */
    }

}
