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

package com.hazelcast.platform.demos.retail.clickstream.job;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Schema;

import com.hazelcast.function.FunctionEx;
import com.hazelcast.function.SupplierEx;
import com.hazelcast.jet.contrib.pulsar.PulsarSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.platform.demos.retail.clickstream.ClickstreamKey;
import com.hazelcast.platform.demos.retail.clickstream.MyConstants;
import com.hazelcast.platform.demos.retail.clickstream.MyUtils;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Simple ingest of Pulsar into Hazelcast map.
 * </p>
 */
@Slf4j
public class PulsarIngest {

    /**
     * <p>Read but don't consume from Pulsar, in case other
     * clusters are reading also.
     * </p>
     */
    public static Pipeline buildPipeline() {
        String serviceUrl = "";
        try {
            serviceUrl = MyUtils.getPulsarServiceUrl();
            log.trace("serviceUrl='{}'", serviceUrl);
        } catch (Exception e) {
            log.error("buildPipeline", e);
            return null;
        }
        final String serviceUrlF = serviceUrl;

        SupplierEx<PulsarClient> pulsarConnectionSupplier =
                () -> PulsarClient.builder()
                .connectionTimeout(1, TimeUnit.SECONDS)
                .serviceUrl(serviceUrlF)
                .build();

        SupplierEx<Schema<String>> pulsarSchemaSupplier =
                () -> Schema.STRING;

        FunctionEx<Message<String>, Message<String>> pulsarProjectionFunction =
                message -> message;

        StreamSource<Message<String>> pulsarSource = PulsarSources.pulsarReaderBuilder(
            MyConstants.PULSAR_TOPIC,
            pulsarConnectionSupplier,
            pulsarSchemaSupplier,
            pulsarProjectionFunction).build();

        Pipeline pipeline = Pipeline.create();

        pipeline
        .readFrom(pulsarSource).withNativeTimestamps(0L)
        .map(message -> {
            String key = message.getKey();
            String event = message.getValue();
            ClickstreamKey clickstreamKey = new ClickstreamKey();
            clickstreamKey.setKey(key);
            clickstreamKey.setPublishTimestamp(message.getPublishTime());
            clickstreamKey.setIngestTimestamp(System.currentTimeMillis());
            return new SimpleImmutableEntry<ClickstreamKey, String>(clickstreamKey, event);
        })
        .writeTo(Sinks.map(MyConstants.IMAP_NAME_CLICKSTREAM));

        return pipeline;
    }
}
