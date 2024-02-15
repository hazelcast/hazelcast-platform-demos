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

package hazelcast.platform.demos.industry.iiot;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.jet.datamodel.Tuple4;

/**
 * <p>Shared utility methods.
 * </p>
 */
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
     * <p>Run a callable that extends {@link Tuple4Callable}.
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
     * @param logger May be Slfj4 or customized
     * @param callable
     * @param hazelcastInstance
     * @param any Run on all members or any one.
     * @return
     */
    static boolean runTuple4Callable(Logger logger, Tuple4Callable callable, HazelcastInstance hazelcastInstance, boolean any) {
        IExecutorService iExecutorService = hazelcastInstance.getExecutorService("default");
        String callableName = callable.getClass().getSimpleName();

        boolean result = true;

        if (any) {
            try {
                List<Tuple4<String, String, List<String>, List<String>>> tuple4List =
                        iExecutorService.submit(callable).get();
                result &= prettyPrint(logger, callableName, "", tuple4List);
            } catch (Exception e) {
                String message = String.format("Submit '%s' to any node:", callableName);
                logger.error(message, e);
                result = false;
            }
        } else {
            Map<Member, Future<List<Tuple4<String, String, List<String>, List<String>>>>> futures =
                    iExecutorService.submitToAllMembers(callable);

            for (Entry<Member, Future<List<Tuple4<String, String, List<String>, List<String>>>>> entry : futures.entrySet()) {
                if (entry.getValue() instanceof Future) {
                    String node = entry.getKey().getAddress().toString();
                    Future<List<Tuple4<String, String, List<String>, List<String>>>> future = entry.getValue();
                    try {
                        List<Tuple4<String, String, List<String>, List<String>>> tuple4List = future.get();
                        result &= prettyPrint(logger, callableName, node, tuple4List);
                    } catch (Exception e) {
                        String message = String.format("Submit '%s' to all nodes, node '%s':", callableName, node);
                        logger.error(message, e);
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
     * @param logger May be Slfj4 or customized
     * @param prefix1
     * @param prefix1
     * @param tuple4list
     * @param True if no errors
     */
    private static boolean prettyPrint(Logger logger, String prefix1, String prefix2,
                List<Tuple4<String, String, List<String>, List<String>>> tuple4list) {
        String prefix = prefix1 + ":";
        if (prefix2 != null && prefix2.length() > 0) {
            prefix = prefix + " " + prefix2 + ":";
        }
        if (tuple4list == null) {
            logger.error("{} QUERY: null result", prefix);
            return false;
        }
        if (tuple4list.isEmpty()) {
            logger.warn("{} QUERY: empty result", prefix);
            return false;
        }
        boolean result = true;
        for (Tuple4<String, String, List<String>, List<String>> tuple4: tuple4list) {
            result &= prettyPrintTuple4(logger, prefix, tuple4);
        }
        return result;
    }

    /**
     * <p>Prints an individual success/failure message.
     * </p>
     *
     * @param logger May be Slfj4 or customized
     * @param prefix
     * @param tuple4
     * @param True if no errors
     */
    private static boolean prettyPrintTuple4(Logger logger, String prefix, Tuple4<String, String, List<String>,
            List<String>> tuple4) {
        boolean ok = true;
        String description = Objects.toString(tuple4.f0());
        if (tuple4.f1() != null && tuple4.f1().length() > 0) {
            description += ", " + tuple4.f1();
        }
        if (tuple4.f2() == null || tuple4.f2().size() == 0) {
            logger.info("{} OK: {}", prefix, description);
        } else {
            logger.error("{} FAIL: {}", prefix, description);
            for (String error : tuple4.f2()) {
                logger.error("  ERROR  =>  '{}'", error);
                ok = false;
            }
        }
        if (tuple4.f3() != null) {
            for (String warning : tuple4.f3()) {
                logger.warn("  WARNING=>  '{}'", warning);
            }
        }
        return ok;
    }

    /**
     * <p>Format an exception to return to client
     * </p>
     *
     * @param errors - modified in-situ
     * @param s - optional, added first
     * @param e - Exception
     */
    public static void addExceptionToError(List<String> errors, String s, Exception e) {
        if (s != null && s.length() > 0) {
            errors.add(s);
        }
        errors.add(e.getMessage());
        try {
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                errors.add(stackTraceElement.toString());
            }
        } catch (Exception e2) {
            errors.add(e2.getMessage());
        }
    }

    /**
     * <p>Format an exception to a logger. Always {@code ERROR}.
     * </p>
     *
     * @param logger - where to log
     * @param s - optional, added first
     * @param e - Exception
     */
    public static void addExceptionToLogger(org.slf4j.Logger logger, String s, Exception e) {
        if (s != null && s.length() > 0) {
            logger.error(s);
        }
        logger.error(e.getMessage());
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            logger.error(stackTraceElement.toString());
        }
    }

    /**
     * <p>Escape JSON to hold in a String
     * </p>
     *
     * @param hazelcastJsonValue
     * @return
     */
    public static String escapeJson(HazelcastJsonValue hazelcastJsonValue) {
        if (hazelcastJsonValue == null) {
            return Objects.toString(null);
        }

        String s = hazelcastJsonValue.toString();
        s = s.replace('"', '`');
        return s;
    }
}
