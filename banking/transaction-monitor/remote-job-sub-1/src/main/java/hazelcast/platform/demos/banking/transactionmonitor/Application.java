/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.map.IMap;

/**
 * <p>Build a single Jet job and submit it to the Hazelcast platform
 * for processing.
 * </p>
 */
public class Application {
    private static final String FILENAME = "application.properties";

    /**
     * <p>Use the Jet connection provided by "{@code Jet.bootstrappedInstance()}"
     * which is a client of the grid, to submit the created job to the grid.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.bootstrappedInstance();

        Properties buildProperties = MyUtils.loadProperties(FILENAME);

        checkFlavor(hazelcastInstance, buildProperties);

        Properties clusterProperties = buildKafkaProperties(hazelcastInstance);
        String buildTimestamp = getBuildTimestamp(buildProperties);

        Pipeline pipelinePythonAnalysis = PythonAnalysis.buildPipeline(clusterProperties, buildTimestamp);

        JobConfig jobConfigPythonAnalysis = new JobConfig();
        jobConfigPythonAnalysis.addClass(PythonAnalysis.class);
        jobConfigPythonAnalysis.setName(PythonAnalysis.class.getSimpleName() + "@" + buildTimestamp);

        // Fails if job exists with same job name, unlike "newJobIfAbsent"
        hazelcastInstance.getJet().newJob(pipelinePythonAnalysis, jobConfigPythonAnalysis);
    }

    /**
     * <p>Check the build flavor matches the cluster.
     * </p>
     *
     * @param hazelcastInstance
     * @throws Exception
     */
    private static void checkFlavor(HazelcastInstance hazelcastInstance, Properties buildProperties) throws Exception {
        IMap<String, String> configMap =
                hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONFIG);

        String buildFlavor = buildProperties.getProperty(MyConstants.TRANSACTION_MONITOR_FLAVOR);
        if (buildFlavor == null || buildFlavor.isBlank()) {
            String message = String.format("'%s' not found in '%s' file",
                    MyConstants.TRANSACTION_MONITOR_FLAVOR, FILENAME);
            System.err.println(message);
            throw new RuntimeException(message);
        }

        String clusterFlavor = configMap.get(MyConstants.TRANSACTION_MONITOR_FLAVOR);
        if (clusterFlavor == null || clusterFlavor.isBlank()) {
            String message = String.format("Map '%s' is empty, run WEBAPP to load",
                    configMap.getName());
            System.err.println(message);
            throw new RuntimeException(message);
        }

        if (!clusterFlavor.equalsIgnoreCase(buildFlavor)) {
            String message = String.format("'%s': Build is '%s', cluster is '%s'",
                    MyConstants.TRANSACTION_MONITOR_FLAVOR, buildFlavor, clusterFlavor);
            System.err.println(message);
            throw new RuntimeException(message);
        }
    }

    /**
     * <p>Retrieve Kafka connection properties from the cached
     * value in the grid, saves having to supply on the command line.
     * Mainly makes sense for the "{@code BOOTSTRAP_SERVERS_CONFIG}".
     * </p>
     *
     * @param hazelcastInstance
     * @return
     */
    private static Properties buildKafkaProperties(HazelcastInstance hazelcastInstance) throws Exception {
        IMap<String, String> configMap =
                hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONFIG);

        if (configMap.isEmpty()) {
            String message = String.format("Map '%s' is empty, run WEBAPP to load",
                    configMap.getName());
            System.err.println(message);
            throw new RuntimeException(message);
        }

        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                configMap.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                configMap.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                configMap.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));

        return properties;
    }


    /**
     * <p>Finds the Maven build from properties from the classpath.
     * </p>
     *
     * @return
     * @throws Exception
     */
    private static String getBuildTimestamp(Properties buildProperties) throws Exception {
        String buildTimestamp = buildProperties.getProperty("my.build-timestamp");
        if (buildTimestamp == null || buildTimestamp.length() == 0) {
            throw new RuntimeException("Could not find 'my.build-timestamp' in 'application.properties'");
        }
        return buildTimestamp;
    }

}
