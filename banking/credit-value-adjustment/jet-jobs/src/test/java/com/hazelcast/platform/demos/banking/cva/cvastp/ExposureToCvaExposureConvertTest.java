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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * <p>Tests for conversion part of
 * {@link com.hazelcast.platform.demos.banking.cva.cvastp.ExposureToCvaExposure ExposureToCvaExposure}
 * </p>
 */
public class ExposureToCvaExposureConvertTest {

    private static final float INPUT1_CDS_RECOVERY = 0.4f;
    private static final float[] INPUT1_CDS_SPREADS =
        {0.0079f, 0.0111f, 0.0128f, 0.0137f, 0.0148f, 0.016f, 0.0174f, 0.0194f, 0.0192f, 0.0195f, 0.0176f};
    private static final float[] INPUT1_CDS_SPREADPERIODS =
        {0.5f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 7.0f, 10.0f, 15.0f, 20.0f, 30.0f};

    private static final double[] INPUT1_EXPOSURE_DISCOUNTFACTORS = {0.999752d, 0.997813523d, 0.994621098d, 0.991563857d};
    private static final double[] INPUT1_EXPOSURE_EXPOSURES = {1.8666573769E7d, 2050721.175d, 1389901.315d, 476799.79799999995d};
    private static final double[] INPUT1_EXPOSURE_LEGFRACTIONS = {0.0597826093d, 0.307065219d, 0.554347813d, 0.809782624d};

    private static final double EXPECTED_CVA = 19237.880232994325d;
    private static final double[] EXPECTED_CVAEXPOSUREBYLEG =
        {8810.243583652038d, 3987.750538023314d, 5116.214695449522d, 1323.671415869447d};
    private static final double[] EXPECTED_DEFAULTPROB =
        {7.868279473475237E-4d, 0.003248035258050619d, 0.006168163564730267d, 0.0046662950266820324d};
    private static final double[] EXPECTED_HAZARDRATES =
        {0.013166666161682865d, 0.013166666161682865d, 0.01849999980131785d, 0.01849999980131785d};
    private static final double[] EXPECTED_SPREADRATES =
        {0.007899999618530273d, 0.007899999618530273d, 0.011099999770522118d, 0.011099999770522118d};

    private static double input1CdsRecoveryRate;
    private static List<Float> input1CdsSpreadPeriodsList;
    private static List<Float> input1CdsSpreadsList;
    private static List<Double> input1ExposureDiscountFactorsList;
    private static List<Double> input1ExposureExposuresList;
    private static List<Double> input1ExposureLegFractionsList;

    private static List<Double> expectedCvaExposureByLegList;
    private static List<Double> expectedDefaultProbList;
    private static List<Double> expectedHazardRatesList;
    private static List<Double> expectedSpreadRatesList;

    @BeforeAll
    public static void beforeAll() throws Exception {
        input1CdsRecoveryRate = (float) INPUT1_CDS_RECOVERY;

        input1CdsSpreadPeriodsList = new ArrayList<>();
        for (int i = 0 ; i < INPUT1_CDS_SPREADPERIODS.length ; i++) {
            input1CdsSpreadPeriodsList.add(INPUT1_CDS_SPREADPERIODS[i]);
        }
        input1CdsSpreadsList = new ArrayList<>();
        for (int i = 0 ; i < INPUT1_CDS_SPREADS.length ; i++) {
            input1CdsSpreadsList.add(INPUT1_CDS_SPREADS[i]);
        }

        input1ExposureDiscountFactorsList = Arrays.stream(INPUT1_EXPOSURE_DISCOUNTFACTORS)
                .boxed().collect(Collectors.toList());
        input1ExposureExposuresList = Arrays.stream(INPUT1_EXPOSURE_EXPOSURES)
                .boxed().collect(Collectors.toList());
        input1ExposureLegFractionsList = Arrays.stream(INPUT1_EXPOSURE_LEGFRACTIONS)
                .boxed().collect(Collectors.toList());
        expectedCvaExposureByLegList = Arrays.stream(EXPECTED_CVAEXPOSUREBYLEG)
                .boxed().collect(Collectors.toList());
        expectedDefaultProbList = Arrays.stream(EXPECTED_DEFAULTPROB)
                .boxed().collect(Collectors.toList());
        expectedHazardRatesList = Arrays.stream(EXPECTED_HAZARDRATES)
                .boxed().collect(Collectors.toList());
        expectedSpreadRatesList = Arrays.stream(EXPECTED_SPREADRATES)
                .boxed().collect(Collectors.toList());
    }

    @Test
    public void testSpreadRates(TestInfo testInfo) {
        List<Double> actual = ExposureToCvaExposure.getSpreadRates(input1CdsSpreadsList,
                input1CdsSpreadPeriodsList, input1ExposureLegFractionsList);
        this.verifyDoubleList(actual, expectedSpreadRatesList, testInfo);
    }

    @Test
    public void testHazardRates(TestInfo testInfo) {
        List<Double> actual = ExposureToCvaExposure.getHazardRates(expectedSpreadRatesList, input1ExposureLegFractionsList,
                input1CdsRecoveryRate);
        this.verifyDoubleList(actual, expectedHazardRatesList, testInfo);
    }

    @Test
    public void testDefaultProb(TestInfo testInfo) {
        List<Double> actual = ExposureToCvaExposure.getDefaultProbabilities(expectedHazardRatesList,
                input1ExposureLegFractionsList);
        this.verifyDoubleList(actual, expectedDefaultProbList, testInfo);
    }

    @Test
    public void testCvaExposureByLeg(TestInfo testInfo) {
        List<Double> actual = ExposureToCvaExposure.getCvaExposureByLeg(expectedDefaultProbList,
                input1ExposureExposuresList, input1ExposureDiscountFactorsList, input1CdsRecoveryRate);
        this.verifyDoubleList(actual, expectedCvaExposureByLegList, testInfo);
    }

    @Test
    public void testCva() {
        double actual = ExposureToCvaExposure.getCvaExposureVal(expectedCvaExposureByLegList);
        assertThat(actual, equalTo(EXPECTED_CVA));
    }

    /**
     * <p>Helper function for comparing double lists.
     * </p>
     */
    private void verifyDoubleList(List<Double> actual, List<Double> expected, TestInfo testInfo) {
        assertThat(testInfo.getDisplayName(), actual, notNullValue());
        assertThat(testInfo.getDisplayName() + ".size", actual.size(), equalTo(expected.size()));

        for (int i = 0 ; i < actual.size() ; i++) {
            Object actualI = actual.get(i);
            Double expectedI = expected.get(i);

            assertThat(testInfo.getDisplayName() + "." + i, actualI, notNullValue());
            assertThat(testInfo.getDisplayName() + "." + i, actualI, instanceOf(Double.class));
            assertThat(testInfo.getDisplayName() + "." + i,
                    actualI, equalTo(expectedI));
        }
    }
}
