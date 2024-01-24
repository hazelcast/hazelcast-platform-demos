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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Returns information about the server. On Viridian, some information may be restricted.
 * </p>
 * <p>NOTE: To emphasise namespaces, this exact same executor is in
 * {@code jar-namespace-1}, {@code jar-namespace-2} and {@code jar-namespace-3}
 * with only the "{@code MY_JAR_NAME}" field and the text capitalisation different.
 */
public class ServerInfoCallable implements Callable<List<String>>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfoCallable.class);

    private final boolean useViridian;
    private final String executor;

    ServerInfoCallable(boolean arg0, String arg1) {
        this.useViridian = arg0;
        this.executor = arg1;
    }

    /**
     * <p>List server information.
     * </p>
     */
    @Override
    public List<String> call() {
        if (!useViridian) {
            LOGGER.info("**{}**'{}'::START call()", LocalConstants.MY_JAR_NAME, this.executor);
        }

        List<String> result = new ArrayList<>();

        try {
            result.add(
                //NOTE: Differs between the three clones of this executable in different namespaces
                // Namespace 1 no text change, namespace 2 upper case, namespace 3 lowercase
                String.format(
                        "**%s**'%s'::Runtime.getRuntime().availableProcessors()==%d classloader='%s' parent.classloader='%s'",
                        LocalConstants.MY_JAR_NAME,
                        this.executor, Runtime.getRuntime().availableProcessors(),
                        this.getClass().getClassLoader().getName(),
                        this.getClass().getClassLoader().getParent().getName()
                        )
                    );
        } catch (Exception e) {
            if (!useViridian) {
                LOGGER.info(String.format("**%s**'%s'::EXCEPTION call()", LocalConstants.MY_JAR_NAME, this.executor), e);
            }
            result.add(e.getMessage());
        }

        if (!useViridian) {
            LOGGER.info("**{}**'{}'::END call()", LocalConstants.MY_JAR_NAME, this.executor);
        }
        return result;
    }

}
