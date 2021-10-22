/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package hazelcast.platform.demos.industry.iiot;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.jet.datamodel.Tuple3;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Shared utility methods.
 * </p>
 */
@Slf4j
public class Utils {

    /**
     * <p>Classes the client sees wrap the real object, so expose it.
     * Client sees "{@code ClientMultiMapProxy}" for "{@code MultiMap}", etc.
     * </p>
     *
     * @param klass
     * @return
     */
    @SuppressWarnings("rawtypes")
    static String formatClientProxyClass(Class klass) {
        String klassName = klass.getSimpleName();

        if (klassName.startsWith("Client")) {
            klassName = klassName.substring("Client".length());
        }
        if (klassName.endsWith("Proxy")) {
            klassName = klassName.substring(0, klassName.length() - "Proxy".length());
        }

        return klassName;
    }

    /**
     * <p>Run a callable that extends {@link Tuple3OutputCallable}.
     * </p>
     * <p>Expected output format:
     * </p>
     * <ol>
     * <li><p><b>f0</b> Main info.
     * </p></li>
     * <li><p><b>f1</b> Optional additional info.
     * </p></li>
     * <li><p><b>f2</b> A list of any errors associated. No errors means it worked.
     * </p></li>
     * </ol>
     *
     * @param callable
     * @param hazelcastInstance
     * @param any Run on all members or any one.
     * @return
     */
    static boolean runTuple3Callable(Tuple3OutputCallable callable, HazelcastInstance hazelcastInstance, boolean any) {
        IExecutorService iExecutorService = hazelcastInstance.getExecutorService("default");
        String callableName = callable.getClass().getSimpleName();

        boolean result = true;

        if (any) {
            try {
                List<Tuple3<String, String, List<String>>> tuple3List =
                        iExecutorService.submit(callable).get();
                prettyPrint(callableName, "", tuple3List);
            } catch (Exception e) {
                String message = String.format("Submit '%s' to any node:", callableName);
                log.error(message, e);
                result = false;
            }
        } else {
            Map<Member, Future<List<Tuple3<String, String, List<String>>>>> futures =
                    iExecutorService.submitToAllMembers(callable);

            for (Entry<Member, Future<List<Tuple3<String, String, List<String>>>>> entry : futures.entrySet()) {
                if (entry.getValue() instanceof Future) {
                    String node = entry.getKey().getAddress().toString();
                    Future<List<Tuple3<String, String, List<String>>>> future = entry.getValue();
                    try {
                        List<Tuple3<String, String, List<String>>> tuple3List = future.get();
                        prettyPrint(callableName, node, tuple3List);
                    } catch (Exception e) {
                        String message = String.format("Submit '%s' to all nodes, node '%s':", callableName, node);
                        log.error(message, e);
                        result = false;
                    }
                }
            }
        }

        return result;
    }

    /**
     * <p>Print the output
     * </p>
     *
     * @param prefix1
     * @param prefix1
     * @param tuple3list
     */
    private static void prettyPrint(String prefix1, String prefix2, List<Tuple3<String, String, List<String>>> tuple3list) {
        String prefix = prefix1 + ":";
        if (prefix2 != null && prefix2.length() > 0) {
            prefix = prefix + " " + prefix2 + ":";
        }
        if (tuple3list == null) {
            log.error("{} QUERY: null result", prefix);
        } else {
            if (tuple3list.isEmpty()) {
                log.warn("{} QUERY: empty result", prefix);

            } else {
                for (Tuple3<String, String, List<String>> tuple3: tuple3list) {
                    if (tuple3.f2() == null || tuple3.f2().size() == 0) {
                        log.info("{} OK: {}, {}", prefix, tuple3.f0(), tuple3.f1());
                    } else {
                        log.error("{} FAIL: {}, {}", prefix, tuple3.f0(), tuple3.f1());
                        for (String line : tuple3.f2()) {
                            log.error("  =>  '{}'", line);
                        }
                    }
                }
            }
        }
    }
}
