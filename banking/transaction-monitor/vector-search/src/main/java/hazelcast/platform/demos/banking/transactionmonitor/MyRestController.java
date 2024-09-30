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

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.vector.SearchOptions;
import com.hazelcast.vector.SearchResult;
import com.hazelcast.vector.SearchResults;
import com.hazelcast.vector.VectorCollection;
import com.hazelcast.vector.VectorValues;
import com.hazelcast.vector.VectorValues.SingleVectorValues;

/**
 * <p>Makes vector search available to Javascript
 * </p>
 */
@RestController
@RequestMapping("/rest")
public class MyRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRestController.class);

    private final VectorCollection<String, String> transactions;

    public MyRestController(HazelcastInstance hazelcastInstance) {
        this.transactions = VectorCollection.getCollection(hazelcastInstance, MyConstants.VECTOR_COLLECTION_TRANSACTIONS);
    }

    /**
     * <p>Return all fixing dates and rates.
     * <p>
     *
     * @return A String which Spring converts into JSON.
     */
    @GetMapping(value = "/vectorsearch", produces = MediaType.APPLICATION_JSON_VALUE)
    public String vectorSearch(@RequestParam("ints") String intCsv) {
        LOGGER.info("vectorSearch(): {}", intCsv);

        boolean error = false;
        String errorMessage = "";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ \"vectorsearch\": [");

        try {
            float[] targetMoments = this.parse(intCsv);
            VectorValues targetVectorValues = VectorValues.of(MyConstants.VECTOR_DOCUMENT_MOMENTS, targetMoments);

            SearchResults<String, String> searchResults =
                    this.transactions
                    .searchAsync(targetVectorValues, SearchOptions.of(MyConstants.SQL_RESULT_THRESHOLD, true, true))
                    .toCompletableFuture().get();

            Iterator<SearchResult<String, String>> iterator  = searchResults.results();
            int count = 0;
            while (iterator.hasNext()) {
                if (count > 0) {
                    stringBuilder.append(" , ");
                }
                stringBuilder.append(formatMatch(iterator.next()));
                count++;
            }
        } catch (Exception e) {
            LOGGER.error(String.format("vectorSearch(%s)", intCsv, e));
            error = true;
            errorMessage = e.getMessage();
        }

        stringBuilder.append(" ]");
        stringBuilder.append(", \"error\": ").append(error);
        stringBuilder.append(", \"error_message\": \"").append(MyUtils.safeForJsonStr(errorMessage)).append("\"");
        stringBuilder.append(" }");
        LOGGER.debug("vectorSearch(): search return {}", stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * <p>Parse CSV input to {@code float[]}.
     * </p>
     *
     * @param intCsv
     * @return
     * @throws Exception
     */
    private float[] parse(String intCsv) throws Exception {
        float[] result = new float[MyConstants.MOMENTS_IN_HOUR];
        String[] input = intCsv.split(",");
        if (input.length != result.length) {
            throw new RuntimeException(String.format("Parse '%s' found %d not %d items",
                    intCsv, input.length, result.length));
        }
        for (int i = 0; i < input.length; i++) {
            try {
                result[i] = Float.parseFloat(input[i]);
            } catch (NumberFormatException nfe) {
                throw new RuntimeException(String.format("Parse '%s' item %d not a number, '%s'",
                        intCsv, i, input[i]));
            }
        }
        return result;
    }

    /**
     * <p>Format one search match for JSON
     * </p>
     *
     * @param next
     * @return
     */
    private String formatMatch(SearchResult<String, String> searchResult) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append(" \"key\" : \"").append(searchResult.getKey()).append("\"");
        stringBuilder.append(",\"score\" : \"").append(searchResult.getScore()).append("\"");

        stringBuilder.append(",\"vector_values\" : [");
        VectorValues vectors = searchResult.getVectors();
        if (vectors instanceof SingleVectorValues) {
            SingleVectorValues singleVectorValues = (SingleVectorValues) vectors;
            float[] vector = singleVectorValues.vector();
            for (int i = 0; i < vector.length; i++) {
                if (i > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(vector[i]);
            }
        } else {
            throw new RuntimeException("vectors class: " + vectors.getClass().getCanonicalName());
        }
        stringBuilder.append(" ]");

        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
