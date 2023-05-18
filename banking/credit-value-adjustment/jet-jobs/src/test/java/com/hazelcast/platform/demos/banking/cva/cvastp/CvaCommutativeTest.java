/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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
import static org.hamcrest.CoreMatchers.not;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * <p>Confirm which Java numeric types adhere to the laws of mathematics,
 * and which go wrong because of rounding errors.
 * </p>
 */
public class CvaCommutativeTest {

    private static final double[] ONE_HUNDRED_CVAS = {
            11193.684938059429d,
            11067.185987493674d,
            10242.499427269157d,
            10915.076295155333d,
            10196.49882633065d,
            10092.82583529769d,
            10530.360040607386d,
            10461.08704264411d,
            10768.601737028877d,
            9874.689931749315d,
            10845.829874312254d,
            10314.06444879575d,
            10913.465797581697d,
            10405.219704324672d,
            10918.076178835208d,
            10935.077012919985d,
            10424.880464710337d,
            10285.533080544203d,
            11148.656558669138d,
            10935.12051651395d,
            10518.492453789004d,
            10961.018084364312d,
            10336.727504927236d,
            10685.464352714476d,
            11187.464878295195d,
            10210.37473701154d,
            10322.424529566533d,
            10602.253214710792d,
            10596.249586654596d,
            10309.725560578067d,
            10233.847828784685d,
            10744.30455268614d,
            10847.38085517711d,
            10640.618915560057d,
            11132.572206000505d,
            11112.067512597136d,
            11754.983277036725d,
            10889.997895269284d,
            11297.475088073223d,
            10450.920441260198d,
            10288.966512493655d,
            10664.441937769689d,
            10509.514383381187d,
            11385.314686418054d,
            10843.347886207204d,
            9963.54658958539d,
            10473.060321041039d,
            10833.718880042978d,
            10709.990584961033d,
            11268.860141730234d,
            10172.506673020944d,
            10468.084782351649d,
            10301.515623555984d,
            10542.561266110044d,
            10074.511863902319d,
            10999.865896197085d,
            10560.708078767024d,
            10676.527404047736d,
            11738.213121410461d,
            10831.190381556167d,
            10378.356325942283d,
            10048.674438263788d,
            10594.468942673402d,
            10440.533729612747d,
            10892.21162915783d,
            10640.671030861811d,
            10942.222663118344d,
            11137.979214295967d,
            10448.082600138852d,
            10976.897221442945d,
            10874.056690444404d,
            9982.495434939516d,
            9973.670846169802d,
            11005.680852462878d,
            11100.22465625867d,
            11510.903812717053d,
            10520.635752593753d,
            10921.857176727264d,
            11112.060012946906d,
            10878.098476259724d,
            10886.804099932388d,
            9952.882786044844d,
            10473.564383058256d,
            11038.350022365797d,
            10323.79541107247d,
            10239.045486316028d,
            10531.020174787072d,
            10832.890021153244d,
            10717.331872440463d,
            10016.380715437193d,
            10997.589391603764d,
            10069.892089437173d,
            10873.186752859126d,
            9879.878072496364d,
            10426.158537873765d,
            10214.240497104767d,
            10676.936312421953d,
            10612.004558037861d,
            10669.09510309484d,
            10459.939574348991d
    };

    /**
     * <p>Sum the CVAs from first to last, and from last to first.
     * </p>
     * <p>According the laws of mathemetics, addition is commutative, so
     * the result should be the same.
     * </p>
     * <p>Test this for "{@code double}" which is known not to be wholly
     * correct, and for "{@link java.math.BigDecimal BigDecimal}" which
     * aims to be.
     * </p>
     */
    @Test
    public void testCommutativeAddition() {
        double countUpD = 0d;
        double countDownD = 0d;
        BigDecimal countUpBD = BigDecimal.ZERO;
        BigDecimal countDownBD = BigDecimal.ZERO;

        for (int i = 0; i < ONE_HUNDRED_CVAS.length; i++) {
            countUpD += ONE_HUNDRED_CVAS[i];
            countUpBD = countUpBD.add(new BigDecimal(ONE_HUNDRED_CVAS[i]));
        }

        for (int i = (ONE_HUNDRED_CVAS.length - 1); i >= 0 ; i--) {
            countDownD += ONE_HUNDRED_CVAS[i];
            countDownBD = countDownBD.add(new BigDecimal(ONE_HUNDRED_CVAS[i]));
        }

        assertThat("double", countDownD, not(equalTo(countUpD)));
        assertThat("BigDecimal", countDownBD, equalTo(countUpBD));
    }
}
