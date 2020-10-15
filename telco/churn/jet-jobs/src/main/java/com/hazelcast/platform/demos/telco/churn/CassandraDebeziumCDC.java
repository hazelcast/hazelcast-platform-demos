/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

//XXX import org.slf4j.Logger;
//XXX import org.slf4j.LoggerFactory;
//XXX import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;

/**
 * XXX
 */
public class CassandraDebeziumCDC {
    private static final String JOB_NAME_PREFIX = CassandraDebeziumCDC.class.getSimpleName();
    //private static final Logger LOGGER = LoggerFactory.getLogger(CassandraDebeziumCDC.class);

    /**
     * <p>Job configuration, mainly which classes need to be distributed
     * to execution nodes.
     * </p>
     */
    public static JobConfig buildJobConfig(String timestampStr) {
        JobConfig jobConfig = new JobConfig();

        jobConfig.setName(JOB_NAME_PREFIX + "@" + timestampStr);

        //XXX jobConfig.addClass(JSONObject.class);

        return jobConfig;
    }

    public static Pipeline buildPipeline() {
        return null;
    }
}
