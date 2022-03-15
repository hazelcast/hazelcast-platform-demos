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

package com.hazelcast.jet.python;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Runs on the target node</p>
 */
@Slf4j
public class DagPythonSupplier implements ProcessorSupplier {
    private static final long serialVersionUID = 1L;

    private final String moduleName;
    private final List<String> module;
    private final String methodName;
    private final List<String> requirements;
    private Context context;

    public DagPythonSupplier(String arg0, List<String> arg1, String arg2, List<String> arg3) {
        this.moduleName = arg0;
        this.module = arg1;
        this.methodName = arg2;
        this.requirements = arg3;
        log.trace("DagPythonSupplier('{}', '{}', '{}', '{}')", MyUtils.truncateToString(this.moduleName),
                MyUtils.truncateToString(this.module), MyUtils.truncateToString(this.methodName),
                MyUtils.truncateToString(this.requirements));
    }

    @Override
    public void init(Context arg0) {
        this.context = arg0;
    }

    @Override
    public Collection<? extends Processor> get(int arg0) {
        List<DagPythonProcessor> l = new ArrayList<>();
        if (arg0 != 1) {
            //TODO: Currently untested, no reason it won't work
            log.error("Parallelism not 1, given {}", arg0);
        } else {
            for (int i = 0 ; i < arg0; i++) {
                try {
                    l.add(new DagPythonProcessor(this.context, this.moduleName, this.module, this.methodName, this.requirements));
                } catch (Exception e) {
                    log.error("get():" + i, e);
                    // No need for more. Jet will abandon the job if less processors than requested
                }
            }
        }
        return l;
    }

}
