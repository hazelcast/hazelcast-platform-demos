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

package hazelcast.platform.demos.banking.trademonitor;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.platform.demos.utils.UtilsProperties;
import com.hazelcast.platform.demos.utils.UtilsSlack;
import com.hazelcast.platform.demos.utils.UtilsSlackSQLJob;

/**
 * <p>Initialise the Jet cluster to ensure the necessary extra parts
 * exist and necessary jobs are running. These are idempotent operations,
 * the will only do anything for the first node in the Jet cluster.
 * </p>
 */
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);
    // Local constant, never needed outside this class
    private static final String APPLICATION_PROPERTIES_FILE = "application.properties";

    /**
     * <p>Ensure the necessary {@link com.hazelcast.core.DistributedObject} exist to
     * hold processing results. Launch the Jet jobs for this example.
     * </p>
     */
    public static void initialise(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarList) throws Exception {

        // Exit if properties not as expected
        Properties properties = null;
        try {
            properties = UtilsProperties.loadClasspathProperties(APPLICATION_PROPERTIES_FILE);
            Properties properties2 = UtilsSlack.loadSlackAccessProperties();
            properties.putAll(properties2);
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName()
                    + " - No jobs submitted for Slack");
            return;
        }

        String pulsarOrKafka = properties.getProperty(MyConstants.PULSAR_OR_KAFKA_KEY);
        boolean usePulsar = MyUtils.usePulsar(pulsarOrKafka);

        CommonIdempotentInitialization.createNeededObjects(hazelcastInstance);
        addListeners(hazelcastInstance, bootstrapServers, pulsarList, usePulsar);
        CommonIdempotentInitialization.loadNeededData(hazelcastInstance, bootstrapServers, pulsarList, usePulsar);
        CommonIdempotentInitialization.defineQueryableObjects(hazelcastInstance, bootstrapServers);

        CommonIdempotentInitialization.launchNeededJobs(hazelcastInstance, bootstrapServers,
                pulsarList, properties);
    }


    /**
     * <p>Logging listeners, and job control.
     * </p>
     *
     * @param hazelcastInstance
     */
    static void addListeners(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarList, boolean usePulsar) {
        MyMembershipListener myMembershipListener = new MyMembershipListener(hazelcastInstance);
        hazelcastInstance.getCluster().addMembershipListener(myMembershipListener);

        JobControlListener jobControlListener =
                new JobControlListener(bootstrapServers, pulsarList, usePulsar);
        hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONTROL)
            .addLocalEntryListener(jobControlListener);
    }

}
