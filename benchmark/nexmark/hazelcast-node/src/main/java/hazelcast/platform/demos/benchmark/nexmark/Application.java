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

package hazelcast.platform.demos.benchmark.nexmark;

import com.hazelcast.core.Hazelcast;

/**
 * <p>Start a Hazelcast server. Configuration will be automatically
 * loaded from named file "{@code hazelcast.yml}". Server will stay
 * running until killed or shutdown from Management Center.
 * Jet processing job is initiated from Hazelcast client.
 * </p>
 */
public class Application {

    public static void main(String[] args) throws Exception {
        System.out.println("-=-=-=-=-");
        System.out.println("Runtime.getRuntime().availableProcessors()==" + Runtime.getRuntime().availableProcessors());
        System.getProperties().list(System.out);
        System.out.println("-=-=-=-=-");

        Hazelcast.newHazelcastInstance();
    }

}
