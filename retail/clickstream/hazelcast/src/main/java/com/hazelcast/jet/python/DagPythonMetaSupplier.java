/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.jet.python;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.hazelcast.cluster.Address;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Runs on the source node</p>
 * <p>Supplies the Python code as a line by line list of strings
 * to the place it will run.
 * </p>
 */
@Slf4j
public class DagPythonMetaSupplier implements ProcessorMetaSupplier {
    private static final long serialVersionUID = 1L;

    private final String moduleName;
    private final String methodName;

    public DagPythonMetaSupplier(String arg0, String arg1) {
        this.moduleName = Objects.toString(arg0);
        this.methodName = Objects.toString(arg1);
        log.trace("DagPythonMetaSupplier('{}', '{}')",
                MyUtils.truncateToString(this.moduleName), MyUtils.truncateToString(this.methodName));
    }

    @Override
    public Function<? super Address, ? extends ProcessorSupplier> get(List<Address> arg0) {
        String subdir = "python" + File.separator;
        List<String> module = this.load(subdir + this.moduleName + ".py", true);
        List<String> requirements = this.load(subdir + "requirements.txt", false);
        return __ -> new DagPythonSupplier(this.moduleName, module, this.methodName, requirements);
    }

    /**
     * <p>Turn a file into a list of lines.
     * </p>
     *
     * @param fileName
     * @param isRequired
     * @return
     */
    private List<String> load(String fileName, boolean isRequired) {
        List<String> results = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                if (isRequired) {
                    throw new RuntimeException(fileName + ": not found in Jar");
                }
            } else {
                try (InputStreamReader inputStreamReader =
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                         String line = bufferedReader.readLine();
                         while (line != null) {
                             // Minor compression
                             if (line.length() > 0 && !line.startsWith("#")) {
                                 results.add(line);
                             }
                             line = bufferedReader.readLine();
                         }
                } catch (Exception e) {
                    String message = String.format("load('%s, %s)", fileName, isRequired);
                    log.error(message, e);
                }
            }
        } catch (Exception e) {
            String message = String.format("load('%s, %s)", fileName, isRequired);
            log.error(message, e);
        }

        log.trace("load('{}', {}) -> {} lines", fileName, isRequired, results.size());
        return results;
    }

    @Override
    public int preferredLocalParallelism() {
        return 1;
    }
}
