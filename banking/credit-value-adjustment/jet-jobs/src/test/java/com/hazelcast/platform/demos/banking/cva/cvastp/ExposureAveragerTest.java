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

package com.hazelcast.platform.demos.banking.cva.cvastp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>Tests for {@link com.hazelcast.platform.demos.banking.cva.cvastp.ExposureAverager ExposureAverager}
 * </p>
 */
public class ExposureAveragerTest {

    private static final double[] INPUT_DISCOUNTFACTORS1 = {0.999752d, 0.997813523d, 0.994621098d, 0.991563857d};
    private static final double[] INPUT_EXPOSURES1 = {1.870426486E7d, 2086160.313d, 1379230.19d, 449535.0804999999d};
    private static final double[] INPUT_LEGFRACTIONS1 = {0.0597826093d, 0.307065219d, 0.554347813d, 0.809782624d};

    private static final double[] INPUT_DISCOUNTFACTORS2 = {0.999722064d, 0.997806072d, 0.995048642d, 0.991720557d};
    private static final double[] INPUT_EXPOSURES2 = {1.8628882678E7d, 2015282.037d, 1400572.44d, 504064.5155d};
    private static final double[] INPUT_LEGFRACTIONS2 = {0.0597826093d, 0.307065219d, 0.554347813d, 0.809782624d};

    private static final double[] EXPECTED_DISCOUNTFACTORS = {0.999752d, 0.997813523d, 0.994621098d, 0.991563857d};
    private static final double[] EXPECTED_EXPOSURES = {1.8666573769E7d, 2050721.175d, 1389901.315d, 476799.79799999995d};
    private static final double[] EXPECTED_LEGFRACTIONS = {0.0597826093d, 0.307065219d, 0.554347813d, 0.809782624d};
    private static final String COUNTERPARTY = "c1";
    private static final String CURVENAME = "";
    private static final String TRADEID = "t1";

    private static String input1;
    private static String input2;
    private static String expectedOutput;
    private static JSONObject expectedOutputJson;
    private static JSONArray expectedOutputJsonFieldNames;

    private static Tuple3<String, String, String> firstExposure;
    private static Tuple3<String, String, String> secondExposure;

    @BeforeClass
    public static void beforeClass() throws Exception {
        input1 = buildTestExposure(INPUT_DISCOUNTFACTORS1, INPUT_EXPOSURES1, INPUT_LEGFRACTIONS1);
        input2 = buildTestExposure(INPUT_DISCOUNTFACTORS2, INPUT_EXPOSURES2, INPUT_LEGFRACTIONS2);
        expectedOutput = buildTestExposure(EXPECTED_DISCOUNTFACTORS, EXPECTED_EXPOSURES, EXPECTED_LEGFRACTIONS);

        expectedOutputJson = new JSONObject(expectedOutput);
        expectedOutputJsonFieldNames = expectedOutputJson.names();

        firstExposure = Tuple3.tuple3(TRADEID, COUNTERPARTY, input1);
        secondExposure = Tuple3.tuple3(TRADEID, COUNTERPARTY, input2);
    }

    public static String buildTestExposure(double[] discountFactors, double[] exposures, double[] legFractions) {
        return "{"
                + " \"tradeid\": \"" + TRADEID + "\""
                + ", \"curvename\": \"" + CURVENAME + "\""
                + ", \"counterparty\": \"" + COUNTERPARTY + "\""
                + ", \"exposures\": " + Arrays.toString(exposures)
                + ", \"legfractions\": " + Arrays.toString(legFractions)
                + ", \"discountfactors\": " + Arrays.toString(discountFactors)
                + "}";
    }

    /**
     * <p>Main end-to-end test. Note some fields in the output come from the first
     * exposure passed as input. The "{@code expectedOutput}" assumes "{@code input1}"
     * then "{@code input2}".
     * See {@link testEndToEndReverseOrderIgnoreLegfractionsAndDiscountfactors()} which
     * tests what happens if the input is the other way round.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testEndToEnd() throws Exception {
        ExposureAverager exposureAverager = new ExposureAverager();

        exposureAverager.accumulate(firstExposure);
        exposureAverager.accumulate(secondExposure);

        String result = exposureAverager.exportFinish();

        assertThat(result, notNullValue());

        JSONObject resultJson = new JSONObject(result);
        JSONArray resultFieldNames = resultJson.names();

        assertThat("names()", expectedOutputJsonFieldNames.length(), equalTo(resultFieldNames.length()));

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            assertThat("has " + fieldName, resultJson.has(fieldName));
        }

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            Object expectedField = expectedOutputJson.get(fieldName);
            Object actualField = resultJson.get(fieldName);
            if (expectedField instanceof JSONArray) {
                assertThat(fieldName, actualField, instanceOf(JSONArray.class));
                JSONArray expectedFieldArray = (JSONArray) expectedField;
                JSONArray actualFieldArray = (JSONArray) actualField;
                assertThat("length " + fieldName, actualFieldArray.length(), equalTo(expectedFieldArray.length()));
                for (int j = 0 ; j < actualFieldArray.length() ; j++) {
                    assertThat(fieldName + "[" + j + "]", actualFieldArray.get(j), equalTo(expectedFieldArray.get(j)));
                }
            } else {
                assertThat(fieldName, actualField.toString(), equalTo(expectedField.toString()));
            }
        }
    }

    /**
     * "{@code legfractions}" and "{@code discountfactors}" are taken from the first exposure passed
     * to the accumulator, so if we reverse the order of input, we can no longer expect these two
     * fields to give the expected answer.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testEndToEndReverseOrderIgnoreLegfractionsAndDiscountfactors() throws Exception {
        ExposureAverager exposureAverager = new ExposureAverager();

        exposureAverager.accumulate(secondExposure);
        exposureAverager.accumulate(firstExposure);

        String result = exposureAverager.exportFinish();

        assertThat(result, notNullValue());

        JSONObject resultJson = new JSONObject(result);
        JSONArray resultFieldNames = resultJson.names();

        assertThat("names()", expectedOutputJsonFieldNames.length(), equalTo(resultFieldNames.length()));

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            assertThat("has " + fieldName, resultJson.has(fieldName));
        }

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            Object expectedField = expectedOutputJson.get(fieldName);
            Object actualField = resultJson.get(fieldName);
            if (expectedField instanceof JSONArray) {
                assertThat(fieldName, actualField, instanceOf(JSONArray.class));
                JSONArray expectedFieldArray = (JSONArray) expectedField;
                JSONArray actualFieldArray = (JSONArray) actualField;
                assertThat("length " + fieldName, actualFieldArray.length(), equalTo(expectedFieldArray.length()));
                for (int j = 0 ; j < actualFieldArray.length() ; j++) {
                    if (!fieldName.equals("discountfactors") && !fieldName.equals("legfractions")) {
                        assertThat(fieldName + "[" + j + "]", actualFieldArray.get(j), equalTo(expectedFieldArray.get(j)));
                    }
                }
            } else {
                assertThat(fieldName, actualField.toString(), equalTo(expectedField.toString()));
            }
        }
    }

}
