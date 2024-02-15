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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>Tests for {@link com.hazelcast.platform.demos.banking.cva.cvastp.TradeExposureAggregator TradeExposureAggregator}
 * </p>
 */
public class TradeExposureAggregatorTest {

    private static final double INPUT1_CVAEXPOSURE = 19209.61172209749d;
    private static final double[] INPUT1_CVAEXPOSUREBYLEG =
        {8828.033012861328d, 4056.664071149816d, 5076.934376801878d, 1247.9802612844665d};
    private static final double[] INPUT1_DEFAULTPROBABILITIES =
        {7.868279473475237E-4d, 0.003248035258050619d, 0.006168163564730267d, 0.0046662950266820324d};
    private static final double[] INPUT1_HAZARDRATES =
        {0.013166666161682865d, 0.013166666161682865d, 0.01849999980131785d, 0.01849999980131785d};
    private static final double[] INPUT1_SPREADRATES =
        {0.007899999618530273d, 0.007899999618530273d, 0.011099999770522118d, 0.011099999770522118d};

    private static final double INPUT2_CVAEXPOSURE = 19268.293471444358d;
    private static final double[] INPUT2_CVAEXPOSUREBYLEG =
        {8792.190878242684d, 3918.807741658891d, 5157.711135356547d, 1399.5837161862353d};
    private static final double[] INPUT2_DEFAULTPROBABILITIES =
        {7.868279473475237E-4d, 0.003248035258050619d, 0.006168163564730267d, 0.0046662950266820324d};
    private static final double[] INPUT2_HAZARDRATES =
        {0.013166666161682865d, 0.013166666161682865d, 0.01849999980131785d, 0.01849999980131785d};
    private static final double[] INPUT2_SPREADRATES =
        {0.007899999618530273d, 0.007899999618530273d, 0.011099999770522118d, 0.011099999770522118d};

    // Average of INPUT1_CVAEXPOSURE + INPUT2_CVAEXPOSURE
    private static final double EXPECTED_CVAEXPOSURE = (INPUT1_CVAEXPOSURE + INPUT2_CVAEXPOSURE) / 2;
    // Average of INPUT1_CVAEXPOSUREBYLEG + INPUT2_CVAEXPOSUREBYLEG
    private static final double EXPECTED_CVAEXPOSUREBYLEG_0 = (INPUT1_CVAEXPOSUREBYLEG[0] + INPUT2_CVAEXPOSUREBYLEG[0]) / 2;
    private static final double EXPECTED_CVAEXPOSUREBYLEG_1 = (INPUT1_CVAEXPOSUREBYLEG[1] + INPUT2_CVAEXPOSUREBYLEG[1]) / 2;
    private static final double EXPECTED_CVAEXPOSUREBYLEG_2 = (INPUT1_CVAEXPOSUREBYLEG[2] + INPUT2_CVAEXPOSUREBYLEG[2]) / 2;
    private static final double EXPECTED_CVAEXPOSUREBYLEG_3 = (INPUT1_CVAEXPOSUREBYLEG[3] + INPUT2_CVAEXPOSUREBYLEG[3]) / 2;
    private static final double[] EXPECTED_CVAEXPOSUREBYLEG =
        {EXPECTED_CVAEXPOSUREBYLEG_0, EXPECTED_CVAEXPOSUREBYLEG_1, EXPECTED_CVAEXPOSUREBYLEG_2, EXPECTED_CVAEXPOSUREBYLEG_3};
    private static final double[] EXPECTED_DEFAULTPROBABILITIES = INPUT1_DEFAULTPROBABILITIES;
    private static final double[] EXPECTED_HAZARDRATES = INPUT1_HAZARDRATES;
    private static final double[] EXPECTED_SPREADRATES = INPUT1_SPREADRATES;

    private static final String COUNTERPARTY = "cp1";
    private static final String CURVENAME = "c";
    private static final String TRADEID = "t1";

    private static String input1;
    private static String input2;
    private static String expectedOutput;
    private static JSONObject expectedOutputJson;
    private static JSONArray expectedOutputJsonFieldNames;

    private static Tuple3<String, String, String> firstExposure;
    private static Tuple3<String, String, String> secondExposure;

    @BeforeAll
    public static void beforeAll() throws Exception {
        String input1Curvename = CURVENAME + "1";
        String input2Curvename = CURVENAME + "2";
        input1 = buildTestExposure(input1Curvename, INPUT1_CVAEXPOSURE, INPUT1_SPREADRATES, INPUT1_HAZARDRATES,
                INPUT1_DEFAULTPROBABILITIES, INPUT1_CVAEXPOSUREBYLEG);
        input2 = buildTestExposure(input2Curvename, INPUT2_CVAEXPOSURE, INPUT2_SPREADRATES, INPUT2_HAZARDRATES,
                INPUT2_DEFAULTPROBABILITIES, INPUT2_CVAEXPOSUREBYLEG);
        expectedOutput = buildTestExposure("", EXPECTED_CVAEXPOSURE, EXPECTED_SPREADRATES, EXPECTED_HAZARDRATES,
                EXPECTED_DEFAULTPROBABILITIES, EXPECTED_CVAEXPOSUREBYLEG);

        expectedOutputJson = new JSONObject(expectedOutput);
        expectedOutputJsonFieldNames = expectedOutputJson.names();

        firstExposure = Tuple3.tuple3(TRADEID, input1Curvename, input1);
        secondExposure = Tuple3.tuple3(TRADEID, input2Curvename, input2);
    }

    public static String buildTestExposure(String curveName, double cvaExposure,
            double[] spreadrates, double[] hazardrates, double[] defaultProbabilities, double[] cvaExposureByLeg) {
        return "{"
                + " \"tradeid\": \"" + TRADEID + "\""
                + ", \"curvename\": \"" + curveName + "\""
                + ", \"counterparty\": \"" + COUNTERPARTY + "\""
                + ", \"cva\": " + cvaExposure
                + ", \"spreadrates\": " + Arrays.toString(spreadrates)
                + ", \"hazardrates\": " + Arrays.toString(hazardrates)
                + ", \"defaultprob\": " + Arrays.toString(defaultProbabilities)
                + ", \"cvaexposurebyleg\": " + Arrays.toString(cvaExposureByLeg)
                + "}";
    }

    /**
     * <p>Main end-to-end test.</p>
     *
     * @throws Exception
     */
    @Test
    public void testEndToEnd(TestInfo testInfo) throws Exception {
        TradeExposureAggregator tradeExposureAggregator = new TradeExposureAggregator();

        tradeExposureAggregator.accumulate(firstExposure);
        tradeExposureAggregator.accumulate(secondExposure);

        this.verifyEndToEnd(tradeExposureAggregator.exportFinish(), testInfo);
    }

    /**
     * <p>Repeat the end-to-end test, reverse order of input, but output must
     * be same as values taking from input with lowest collating sequence.</p>
     *
     * @throws Exception
     */
    @Test
    public void testEndToEndReverseOrder(TestInfo testInfo) throws Exception {
        TradeExposureAggregator tradeExposureAggregator = new TradeExposureAggregator();

        tradeExposureAggregator.accumulate(secondExposure);
        tradeExposureAggregator.accumulate(firstExposure);

        this.verifyEndToEnd(tradeExposureAggregator.exportFinish(), testInfo);
    }

    public void verifyEndToEnd(Tuple2<String, String> result, TestInfo testInfo) throws Exception {
        assertThat(testInfo.getDisplayName(), result, notNullValue());

        String resultCounterparty = result.f0();
        assertThat(testInfo.getDisplayName() + ".counterparty", resultCounterparty, equalTo(COUNTERPARTY));

        JSONObject resultJson = new JSONObject(result.f1());
        JSONArray resultFieldNames = resultJson.names();

        assertThat(testInfo.getDisplayName() + ".names()",
                expectedOutputJsonFieldNames.length(), equalTo(resultFieldNames.length()));

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            assertThat(testInfo.getDisplayName() + ".has " + fieldName, resultJson.has(fieldName));
        }

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            Object expectedField = expectedOutputJson.get(fieldName);
            Object actualField = resultJson.get(fieldName);
            if (expectedField instanceof JSONArray) {
                assertThat(testInfo.getDisplayName() + "." + fieldName,
                        actualField, instanceOf(JSONArray.class));

                JSONArray expectedFieldArray = (JSONArray) expectedField;
                JSONArray actualFieldArray = (JSONArray) actualField;

                assertThat(testInfo.getDisplayName() + ".length " + fieldName,
                        actualFieldArray.length(), equalTo(expectedFieldArray.length()));

                for (int j = 0 ; j < actualFieldArray.length() ; j++) {
                    assertThat(testInfo.getDisplayName() + "." + fieldName + "[" + j + "]",
                            actualFieldArray.get(j), equalTo(expectedFieldArray.get(j)));
                }
            } else {
                assertThat(testInfo.getDisplayName() + "." + fieldName,
                        actualField.toString(), equalTo(expectedField.toString()));
            }
        }
    }

}
