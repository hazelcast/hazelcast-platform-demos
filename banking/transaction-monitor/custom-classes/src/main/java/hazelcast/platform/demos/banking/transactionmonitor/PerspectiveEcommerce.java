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

package hazelcast.platform.demos.banking.transactionmonitor;

/**
 * <p>Aggregate query results, reformatted from {@link Tuple3}
 * for Finos Perspective viewer.
 * </p>
 * <p>
 * Note this is a simple class, no serialization logic defined
 * (see {@link PerspectiveTradeSerializer}).
 * </p>
 */
public class PerspectiveEcommerce {

    private String code;
    private long count;
    private double sum;
    private double average;
    private int seconds;
    private int random;

    // Generated getters/setters and toString()

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public long getCount() {
        return count;
    }
    public void setCount(long count) {
        this.count = count;
    }
    public double getSum() {
        return sum;
    }
    public void setSum(double sum) {
        this.sum = sum;
    }
    public double getAverage() {
        return average;
    }
    public void setAverage(double average) {
        this.average = average;
    }
    public int getSeconds() {
        return seconds;
    }
    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }
    public int getRandom() {
        return random;
    }
    public void setRandom(int random) {
        this.random = random;
    }
    @Override
    public String toString() {
        return "PerspectiveEcommerce [code=" + code + ", count=" + count + ", sum=" + sum + ", average=" + average
                + ", seconds=" + seconds + ", random=" + random + "]";
    }

}
