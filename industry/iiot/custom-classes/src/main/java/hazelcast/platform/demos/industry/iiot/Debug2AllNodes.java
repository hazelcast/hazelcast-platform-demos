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

package hazelcast.platform.demos.industry.iiot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>Idempotent debug callable, to check information on loaded packages.
 * </p>
 */
public class Debug2AllNodes implements Callable<List<String>>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * <p>Return class information. Our classes are not in the "{@code com}" tree.
     * </p>
     */
    @Override
    public List<String> call() throws Exception {
        List<String> result = new ArrayList<>();

        try {
            ClassLoader classLoader = this.getClass().getClassLoader();

            result.add(String.format("classLoader.getName()==%s", classLoader.getName()));

            for (Package p : classLoader.getDefinedPackages()) {
                if (!p.getName().startsWith("com.")
                        && !p.getName().startsWith("javax.")
                        && !p.getName().startsWith("org.")) {
                    result.add(String.format("package.getName()==%s", p.getName()));
                }
            }

        } catch (Exception e) {
            result.add(e.getMessage());
        }

        return result;
    }

}
