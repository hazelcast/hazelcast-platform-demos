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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;

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

        ClientConfig clientConfig = ApplicationConfig.buildJetClientConfig();

        JetInstance jetInstance = Jet.newJetClient(clientConfig);

        new ApplicationRunner(jetInstance).run();

        jetInstance.shutdown();
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
