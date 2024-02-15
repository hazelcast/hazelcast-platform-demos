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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.map.IMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <p>Idempotent initialization steps to be run on every node
 * before initialization on any node.
 * </p>
 * <p>Ie. member specific runs before cluster specific.
 * </p>
 * <ol>
 * <li>
 * <p>Logging</p>
 * <p>Activate <a href="http://www.slf4j.org/">Slf4j</a>. Logs
 * go to an {@link IMap} for later inspection/extraction.
 * </li>
 * </ol>
 */
public class InitializerAllNodes
    extends Tuple4Callable {
        private static final long serialVersionUID = 1L;
        private static final String MY_NAME = InitializerAllNodes.class.getSimpleName();


    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
            justification = "setHazelcastInstance() gets called by Hazelcast")
    private transient HazelcastInstance hazelcastInstance;
    private Level level;

    InitializerAllNodes(Level arg0) {
        this.level = arg0;
    }

    /**
     * <p>This to do to initialize every node. Each method invoked should
     * be idempotent.
     * </p>
     */
    @Override
    public List<Tuple4<String, String, List<String>, List<String>>> call() throws Exception {
        List<Tuple4<String, String, List<String>, List<String>>> result = new ArrayList<>();

        // Logging is via Slf4j.
        this.initialiseLogging(result);

        return result;
    }


    /**
     * <p>Activate Logging to IMap.
     * </p>
     *
     * @param result
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
            justification = "SpotBugs 4.4.2.2 doesn't spot exception that can be thrown by Logger casting")
    private void initialiseLogging(List<Tuple4<String, String, List<String>, List<String>>> result) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            IMapLoggerFactory.setLevel(this.level);

            Logger logger = IMapLoggerFactory.getLogger(this.getClass());

            logger.info("Runtime.getRuntime().availableProcessors()=="
                        + Runtime.getRuntime().availableProcessors());
        } catch (Exception e) {
            Utils.addExceptionToError(errors, null, e);
        }

        result.add(Tuple4.tuple4(MY_NAME + ".initialiseLogging()", "", errors, warnings));
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    @Override
    public void setHazelcastInstance(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

}
