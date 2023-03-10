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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.platform.demos.utils.UtilsProperties;

/**
 * <p>Entry point, "{@code main()}" method.
 * </p>
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final int DEFAULT_PORT = 8080;

    private static int port;

    /**
     * <p>Configure Hazelcast logging via Slf4j. Implementation
     * in "{@code pom.xml}" is Logback.
     * </p>
     * <p>Set this before Hazelcast starts rather than in
     * "{@code hazelcast-client.yml}", otherwise some log messages
     * are produced before "{@code hazelcast-client.yml}" is read
     * dictating the right logging framework to use.
     * </p>
     */
    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
    }

    /**
     * <p>Create a connection to Jet, start the web application,
     * and should this ever complete, disconnect from Jet.
     * </p>
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            port = DEFAULT_PORT;
        } else {
            port = Integer.parseInt(args[0]);
            LOGGER.debug("Using port {} from command line argument.", port);
        }

        Properties applicationProperties = UtilsProperties.loadClasspathProperties(MyConstants.APPLICATION_PROPERTIES_FILE);
        String propertyName1 = "my.bootstrap.servers";
        String propertyName2 = MyConstants.PROJECT_MODULE;
        String propertyName3 = MyConstants.PULSAR_CONFIG_KEY;
        String propertyName4 = MyConstants.POSTGRES_CONFIG_KEY;
        String bootstrapServers = System.getProperty(propertyName1, "");
        String moduleName = applicationProperties.getProperty(propertyName2, "");
        String pulsarList = System.getProperty(propertyName3, "");
        String postgresAddress = System.getProperty(propertyName4, "");
        TransactionMonitorFlavor transactionMonitorFlavor = MyUtils.getTransactionMonitorFlavor(applicationProperties);

        if (bootstrapServers.isBlank()) {
            LOGGER.error("No value for " + propertyName1);
            System.exit(1);
        }
        if (moduleName.isBlank()) {
            LOGGER.error("No value for " + propertyName2);
            System.exit(1);
        }
        if (pulsarList.isBlank()) {
            LOGGER.error("No value for " + propertyName3);
            System.exit(1);
        }
        if (postgresAddress.isBlank()) {
            LOGGER.error("No value for " + propertyName4);
            System.exit(1);
        }
        LOGGER.info("'bootstrapServers'=='{}'", bootstrapServers);
        LOGGER.info("'moduleName'=='{}'", moduleName);
        LOGGER.info("'pulsarList'=='{}'", pulsarList);
        LOGGER.info("'postgresAddress'=='{}'", postgresAddress);
        LOGGER.info("TransactionMonitorFlavor=='{}'", transactionMonitorFlavor);

        ClientConfig clientConfig = ApplicationConfig.buildClientConfig();

        HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

        try {
            new ApplicationRunner(hazelcastInstance, transactionMonitorFlavor, moduleName).run();
        } catch (Exception e) {
            LOGGER.error("main()", e);
        }

        hazelcastInstance.shutdown();
        System.exit(0);
    }

    /**
     * <p>Return the port the web server is to use.
     * </p>
     *
     * @return Probably 8080, standard port for HTTP
     */
    public static int getPort() {
        return port;
    }

}
