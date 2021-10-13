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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.Processor;
import com.hazelcast.jet.core.ProcessorSupplier;

/**
 * <p>Runs on the target node</p>
 */
public class ModelSupplier implements ProcessorSupplier {
    private static final long serialVersionUID = 1L;

    private transient HazelcastInstance hazelcastInstance;

    @Override
    public void init(Context context) {
        this.hazelcastInstance = context.hazelcastInstance();
    }

    @Override
    public Collection<? extends Processor> get(int arg0) {
        List<ModelProcessor> l = new ArrayList<>();
        for (int i = 0 ; i < arg0; i++) {
            l.add(new ModelProcessor(this.hazelcastInstance));
        }
        return l;
    }
}
