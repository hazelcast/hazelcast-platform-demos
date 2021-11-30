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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.jet.datamodel.Tuple4;

/**
 * <p>A callable that changes nothing, but writes a log message to show
 * it has executed.
 * </p>
 */
public class LoggingNoOpAnyNode extends Tuple4Callable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNoOpAnyNode.class);
    private static final String MY_NAME = LoggingNoOpAnyNode.class.getSimpleName();

    private final String caller;

    LoggingNoOpAnyNode(String arg0) {
        this.caller = arg0;
    }

    @Override
    public List<Tuple4<String, String, List<String>, List<String>>> call() throws Exception {
        List<Tuple4<String, String, List<String>, List<String>>> result = new ArrayList<>();

        LOGGER.info("call() for " + this.caller);

        result.add(Tuple4.tuple4(MY_NAME + ".call()", "", new ArrayList<>(), new ArrayList<>()));
        return result;
    }

}
