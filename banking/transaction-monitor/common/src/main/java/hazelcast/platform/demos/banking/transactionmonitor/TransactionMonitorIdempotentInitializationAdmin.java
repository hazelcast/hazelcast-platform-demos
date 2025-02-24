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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.List;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;

/**
 * <p>May be invoked from clientside or serverside to ensure serverside ready.
 * </p>
 * <p>Has to be idempotent, so a client can call at start-up without
 * having to test if another client has already run it.
 * </p>
 */
public class TransactionMonitorIdempotentInitializationAdmin {

    /**
     * <p>Maps and mappings needed for WAN, rather than replicate "{@code __sql.catalog}"
     * </p>
     */
    public static boolean createMinimal(HazelcastInstance hazelcastInstance,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean useHzCloud, boolean localhost) {
        boolean ok;
        if (localhost) {
            if (useHzCloud) {
                throw new RuntimeException("localhost==true and useHzCloud==true");
            }
            ok = defineAdminIMaps(hazelcastInstance, useHzCloud);
            ok &= launchAdminRunners(hazelcastInstance, transactionMonitorFlavor, useHzCloud);
            ok &= TransactionMonitorIdempotentInitialization.defineWANIMaps(hazelcastInstance, transactionMonitorFlavor);
        } else {
            ok = TransactionMonitorIdempotentInitialization.defineWANIMaps(hazelcastInstance, transactionMonitorFlavor);
        }
        MyUtils.logStuff(hazelcastInstance);
        MyUtils.showMappingsAndViews(hazelcastInstance);
        return ok;
    }

    /**
     * <p>Mappings only for self-administration.
     * <p>
     *
     * @param hazelcastInstance
     * @return
     */
    static boolean defineAdminIMaps(HazelcastInstance hazelcastInstance, boolean useHzCloud) {
        String definition1 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_HEAP
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Long.class.getName() + "'"
                + " )";

        List<String> definitions = List.of(definition1);
        boolean ok = TransactionMonitorIdempotentInitialization.runDefine(definitions, hazelcastInstance);
        return ok;
    }

    /**
     * <p>Submit runnables to update data.
     * </p>
     *
     * @param hazelcastInstance
     * @return
     */
    public static boolean launchAdminRunners(HazelcastInstance hazelcastInstance,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean useHzCloud) {
        IExecutorService iExecutorService = hazelcastInstance.getExecutorService("default");

        // Runs on one node, data for all
        PerspectiveUpdater perspectiveUpdater
            = new PerspectiveUpdater(transactionMonitorFlavor, useHzCloud);
        iExecutorService.execute(perspectiveUpdater);

        // Runs on all nodes, data for all sourced from each
        HeapUpdater heapUpdater = new HeapUpdater(useHzCloud);
        iExecutorService.executeOnAllMembers(heapUpdater);

        // Cannot currently fail, but may be extended in future
        return true;
    }

}
