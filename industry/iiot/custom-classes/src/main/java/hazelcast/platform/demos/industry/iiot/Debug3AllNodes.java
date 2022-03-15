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

package hazelcast.platform.demos.industry.iiot;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>
 * Idempotent debug callable, to prove remote execution works.
 * </p>
 */
public class Debug3AllNodes implements Callable<List<String>>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String FILENAME = "my.properties";

    /**
     * <p>
     * Confirm build information
     * </p>
     */
    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "getResourceAsStream can throw exception")
    public List<String> call() throws Exception {
        List<String> result = new ArrayList<>();

        ClassLoader classLoader = this.getClass().getClassLoader();

        // Maven build. But with multiple Jars on classpath order may be changed
        /*XXX
        Enumeration<URL> enumeration = classLoader.getResources("META-INF/MANIFEST.MF");
        if (!enumeration.hasMoreElements()) {
            result.add("No enumeration for manifest");
        } else {
            URL url = enumeration.nextElement();
            try (InputStream inputStream = url.openStream()) {
                Manifest manifest = new Manifest(inputStream);
                Attributes attributes = manifest.getMainAttributes();
                attributes.entrySet().stream().forEach(e -> result.add(e.toString()));
            }
        }*/

        Properties properties = new Properties();
        try (InputStream inputStream = classLoader.getResourceAsStream(FILENAME)) {
            properties.load(inputStream);
            if (properties.isEmpty()) {
                result.add("'" + FILENAME + "' is empty");
            }
            for (Entry<?, ?> entry : properties.entrySet()) {
                String message = String.format("\"%s\"==\"%s\"", entry.getKey().toString(), entry.getValue().toString());
                result.add(message);
            }
        } catch (Exception e) {
            result.add("'" + FILENAME + "' exception:" + e.getMessage());
        }

        return result;
    }

}
