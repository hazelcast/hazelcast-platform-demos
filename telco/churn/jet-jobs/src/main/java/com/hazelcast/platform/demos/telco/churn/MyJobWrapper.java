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

package com.hazelcast.platform.demos.telco.churn;

import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * <p>Force a standard style on jobs, mainly around naming
 * convention.
 * </p>
 */
public abstract class MyJobWrapper {

    private long timestamp;

    MyJobWrapper(long arg0) {
        this.timestamp = arg0;
    }

    /**
     * <p>If the concrete class is called "{@code KafkaIngest}"
     * then this builds a job config with the name
     * "{@code KafkaIngest@2020-10-22T08:19:26}".
     * </p>
     * <p>This makes it easier to find on the Management Center,
     * and if only one instance of this job is allowed, easy
     * to check running jobs by name prefix to see if one of
     * these is already running.
     * </p>
     * <p>The concrete class may extend {@link JobConfig} with
     * the addition of other classes needing serialized as part
     * of job distribution to the cluster.
     * </p>
     */
    JobConfig getJobConfig() {
        JobConfig jobConfig = new JobConfig();

        var timestampStr = MyUtils.timestampToISO8601(this.timestamp);

        jobConfig.setName(this.getClass().getSimpleName() + "@" + timestampStr);

        jobConfig.addClass(this.getClass());
        jobConfig.addClass(MyJobWrapper.class);

        return jobConfig;
    }

    abstract Pipeline getPipeline();
}
