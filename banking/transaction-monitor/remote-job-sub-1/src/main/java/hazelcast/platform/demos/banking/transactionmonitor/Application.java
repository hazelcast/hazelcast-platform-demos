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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Properties;
import java.util.UUID;

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

    /**
     * <p>Use the Jet connection provided by "{@code Jet.bootstrappedInstance()}"
     * which is a client of the grid, to submit the created job to the grid.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.bootstrappedInstance();

        Properties properties = buildKafkaProperties(hazelcastInstance);
        String buildTimestamp = getBuildTimestamp();

        Pipeline pipelinePythonAnalysis = PythonAnalysis.buildPipeline(properties, buildTimestamp);

        JobConfig jobConfigPythonAnalysis = new JobConfig();
        jobConfigPythonAnalysis.addClass(PythonAnalysis.class);
        jobConfigPythonAnalysis.setName(PythonAnalysis.class.getSimpleName() + "@" + buildTimestamp);

        // Fails if job exists with same job name, unlike "newJobIfAbsent"
        hazelcastInstance.getJet().newJob(pipelinePythonAnalysis, jobConfigPythonAnalysis);
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
        IMap<String, String> kafkaConfigMap =
                hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONFIG);

        if (kafkaConfigMap.isEmpty()) {
            String message = String.format("Map '%s' is empty, run WEBAPP to load",
                    kafkaConfigMap.getName());
            System.err.println(message);
            throw new RuntimeException(message);
        }

        Properties properties = new Properties();

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaConfigMap.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                kafkaConfigMap.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                kafkaConfigMap.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));

        return properties;
    }


    /**
     * <p>Finds the Maven build from the classpath.
     * </p>
     *
     * @return
     * @throws Exception
     */
    private static String getBuildTimestamp() throws Exception {
        Properties properties = MyUtils.loadProperties("application.properties");
        String buildTimestamp = properties.getProperty("my.build-timestamp");
        if (buildTimestamp == null || buildTimestamp.length() == 0) {
            throw new RuntimeException("Could not find 'my.build-timestamp' in 'application.properties'");
        }
        return buildTimestamp;
    }

}
