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

import java.io.Serializable;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.map.EntryProcessor;
//FIXME import com.hazelcast.nio.serialization.GenericRecord;

/**
 * FIXME remove this class, just to test Generic Record
 */
public class MyXXXEntryProcessor implements EntryProcessor<Integer, Object, String>, Serializable {
    /**
    *
    */
   private static final long serialVersionUID = 1L;
   private static final Logger LOGGER = LoggerFactory.getLogger(MyXXXEntryProcessor.class);

   @Override
   public String process(Entry<Integer, Object> entry) {
       LOGGER.warn("MyXXXEntryProcessor().{} PROCESS KEY {}", this, entry.getKey());
       Object value = entry.getValue();
       String result = "KEY " + entry.getKey() + " VALUE " + value.getClass().getCanonicalName();
       /*FIXME needs IMDG 4.1
       if (value instanceof GenericRecord) {
           GenericRecord genericRecord = (GenericRecord) value;
           LOGGER.warn("MyXXXEntryProcessor().{} PROCESS VALUE {} {}", this, entry.getKey(), genericRecord);
           result += " GenericRecord " + genericRecord;
       }*/
       return result;
   }

}
