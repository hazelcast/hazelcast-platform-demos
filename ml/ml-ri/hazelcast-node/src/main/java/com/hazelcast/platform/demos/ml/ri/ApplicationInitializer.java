/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import java.util.Locale;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.topic.ITopic;

/**
 * <p>Initialise the Jet cluster to ensure the necessary extra parts
 * exist and necessary jobs are running. These are idempotent operations,
 * the will only do anything for the first node in the Jet cluster.
 * </p>
 */
public class ApplicationInitializer {

    /**
     * <p>Ensure the necessary {@link com.hazelcast.core.DistributedObject} exist to
     * hold processing results. Listen for job publishing output. Launch the
     * pre-requisite Jet jobs for this example.
     * </p>
     */
    public static void initialise(HazelcastInstance hazelcastInstance) {
        createNeededObjects(hazelcastInstance);
        addNeededListeners(hazelcastInstance);
        launchNeededJobs(hazelcastInstance);
    }


    /**
     * <p>Access the {@link com.hazelcast.map.IMap} and other objects
     * that are used by the example. This will create them on first
     * access, so ensuring all are visible from the outset.
     * </p>
     */
    static void createNeededObjects(HazelcastInstance hazelcastInstance) {
        hazelcastInstance.getMap("points");
        hazelcastInstance.getTopic("pi");
    }


    /**
     * <p>Attach a listener on this node for topic events that
     * are published to topics with the selected names. These
     * topics are created in the previous method. Use one
     * listener for all, as low volume expected.
     * </p>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void addNeededListeners(HazelcastInstance hazelcastInstance) {
        MyTopicListener myTopicListener = new MyTopicListener();

        hazelcastInstance
        .getDistributedObjects()
        .stream()
        .filter(distributedObject -> distributedObject instanceof ITopic)
        .filter(distributedObject -> distributedObject.getName().toLowerCase(Locale.ROOT).contains("pi"))
        .forEach(distributedObject -> {
            ITopic iTopic = (ITopic) distributedObject;

            iTopic.addMessageListener(myTopicListener);
        });
    }


    /**
     * <p>An input source is needed for the example jobs
     * to process.
     * </p>
     * <p>Create one job per cluster that generates X &amp; Y
     * co-ordinates to the map "{@code points}".
     * </p>
     */
    static void launchNeededJobs(HazelcastInstance hazelcastInstance) {
        Pipeline pipeline = RandomXYGenerator.buildPipeline();

        JobConfig jobConfig = new JobConfig();
        jobConfig.setName(RandomXYGenerator.class.getSimpleName());

        hazelcastInstance.getJet().newJobIfAbsent(pipeline, jobConfig);
    }

}
