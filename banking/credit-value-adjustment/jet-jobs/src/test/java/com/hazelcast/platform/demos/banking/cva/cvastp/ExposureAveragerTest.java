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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
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

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws Exception {
        String inputCurvename1 = CURVENAME + "1";
        String inputCurvename2 = CURVENAME + "2";
        input1 = buildTestExposure(INPUT_DISCOUNTFACTORS1, INPUT_EXPOSURES1, INPUT_LEGFRACTIONS1, inputCurvename1);
        input2 = buildTestExposure(INPUT_DISCOUNTFACTORS2, INPUT_EXPOSURES2, INPUT_LEGFRACTIONS2, inputCurvename1);
        expectedOutput = buildTestExposure(EXPECTED_DISCOUNTFACTORS, EXPECTED_EXPOSURES, EXPECTED_LEGFRACTIONS, "");

        expectedOutputJson = new JSONObject(expectedOutput);
        expectedOutputJsonFieldNames = expectedOutputJson.names();

        firstExposure = Tuple3.tuple3(TRADEID, inputCurvename1, input1);
        secondExposure = Tuple3.tuple3(TRADEID, inputCurvename2, input2);
    }

    public static String buildTestExposure(double[] discountFactors, double[] exposures, double[] legFractions,
            String curveName) {
        return "{"
                + " \"tradeid\": \"" + TRADEID + "\""
                + ", \"curvename\": \"" + curveName + "\""
                + ", \"counterparty\": \"" + COUNTERPARTY + "\""
                + ", \"exposures\": " + Arrays.toString(exposures)
                + ", \"legfractions\": " + Arrays.toString(legFractions)
                + ", \"discountfactors\": " + Arrays.toString(discountFactors)
                + "}";
    }

    /**
     * <p>Main end-to-end test.</p>
     *
     * @throws Exception
     */
    @Test
    public void testEndToEnd() throws Exception {
        ExposureAverager exposureAverager = new ExposureAverager();

        exposureAverager.accumulate(firstExposure);
        exposureAverager.accumulate(secondExposure);

        this.verifyEndToEnd(exposureAverager.exportFinish());
    }

    /**
     * <p>Repeat the end-to-end test, reverse order of input, but output must
     * be same as values taking from input with lowest collating sequence.</p>
     *
     * @throws Exception
     */
    @Test
    public void testEndToEndReverseOrder() throws Exception {
        ExposureAverager exposureAverager = new ExposureAverager();

        exposureAverager.accumulate(secondExposure);
        exposureAverager.accumulate(firstExposure);

        this.verifyEndToEnd(exposureAverager.exportFinish());
    }

    public void verifyEndToEnd(String result) throws Exception {
        assertThat(this.testName.getMethodName(), result, notNullValue());

        JSONObject resultJson = new JSONObject(result);
        JSONArray resultFieldNames = resultJson.names();

        assertThat(this.testName.getMethodName() + ".names()",
                expectedOutputJsonFieldNames.length(), equalTo(resultFieldNames.length()));

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            assertThat(this.testName.getMethodName() + ".has " + fieldName, resultJson.has(fieldName));
        }

        for (int i = 0 ; i < expectedOutputJsonFieldNames.length() ; i++) {
            String fieldName = expectedOutputJsonFieldNames.getString(i);
            Object expectedField = expectedOutputJson.get(fieldName);
            Object actualField = resultJson.get(fieldName);
            if (expectedField instanceof JSONArray) {
                assertThat(this.testName.getMethodName() + "." + fieldName,
                        actualField, instanceOf(JSONArray.class));

                JSONArray expectedFieldArray = (JSONArray) expectedField;
                JSONArray actualFieldArray = (JSONArray) actualField;

                assertThat(this.testName.getMethodName() + ".length " + fieldName,
                        actualFieldArray.length(), equalTo(expectedFieldArray.length()));

                for (int j = 0 ; j < actualFieldArray.length() ; j++) {
                    assertThat(this.testName.getMethodName() + "." + fieldName + "[" + j + "]",
                            actualFieldArray.get(j), equalTo(expectedFieldArray.get(j)));
                }
            } else {
                assertThat(this.testName.getMethodName() + "." + fieldName,
                        actualField.toString(), equalTo(expectedField.toString()));
            }
        }
    }

}
