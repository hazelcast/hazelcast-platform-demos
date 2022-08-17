/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hazelcast.jet.pipeline.JoinClause;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;

/**
 * <p>Helper functions for the {@code CvaStpJob}
 * </p>
 */
public class CvaStpUtils {

    private static final double FIVE_ATTEMPTS = 5d;
    private static final int TEN_THOUSAND = 10_000;

    /**
     * <p>
     * A join clause for the cartesian product.
     * </p>
     * <p>
     * The left side extractor ignores the input object and always returns the same
     * value. The right side extractor ignores it's input object and always returns
     * the same value, and this is the same value as the left. So both match,
     * always.
     * </p>
     *
     * @return An "<i>always true</i>" function.
     */
    public static JoinClause<Boolean, Object, Object, Object> cartesianProduct() {
        return JoinClause.onKeys(__ -> true, __ -> true);
    }

    /**
    * <p>Create a String for an Exposure, based on input. Input is all Java
    * fields, unlike counterpart {@link makeExposureStrFromJson} which
    * tolerates a bit of JSON on the input.
    * </p>
    *
    * @param tradeid A String
    * @param curvename A String
    * @param counterparty A String
    * @param exposures Array of doubles
    * @param legfractions Array of doubles
    * @param discountfactors Array of doubles
    * @return A string for the exposure, that can be turned into JSON directory.
     */
    public static String makeExposureStrFromJava(String tradeid, String curvename, String counterparty,
           double[] exposures, double[] legfractions, double[] discountfactors) {

       StringBuilder stringBuilder = new StringBuilder();
       stringBuilder.append("{");
       stringBuilder.append(" \"tradeid\": \"" + tradeid + "\"");
       stringBuilder.append(", \"curvename\": \"" + curvename + "\"");
       stringBuilder.append(", \"counterparty\": \"" + counterparty + "\"");

       stringBuilder.append(", \"exposures\": [");
       for (int i = 0 ; i < exposures.length; i++) {
           if (i > 0) {
               stringBuilder.append(", ");
           }
           stringBuilder.append(exposures[i]);
       }
       stringBuilder.append("]");

       stringBuilder.append(", \"legfractions\": [");
       for (int i = 0 ; i < legfractions.length; i++) {
           if (i > 0) {
               stringBuilder.append(", ");
           }
           stringBuilder.append(legfractions[i]);
       }
       stringBuilder.append("]");

       stringBuilder.append(", \"discountfactors\": [");
       for (int i = 0 ; i < discountfactors.length; i++) {
           if (i > 0) {
               stringBuilder.append(", ");
           }
           stringBuilder.append(discountfactors[i]);
       }
       stringBuilder.append("]");

       stringBuilder.append(" }");

       return stringBuilder.toString();
   }

   /**
    * <p>Create a String for an Exposure, based on input. Note that "discountvalues"
    * on input becomes "discountfactors" on output.
    * Use {@link makeExposureStrFromJava()} to do most of the work.
    * </p>
    *
    * @param tradeid A String
    * @param curvename A String
    * @param counterparty A String
    * @param exposures Array of doubles
    * @param mtmJson From the original MTM
    * @return A string for the exposure, that can be turned into JSON directory.
    */
    public static String makeExposureStrFromJson(String tradeid, String curvename, String counterparty,
            double[] exposures, JSONObject mtmJson) throws JSONException {
        JSONArray legfractionsJson = mtmJson.getJSONArray("legfractions");
        JSONArray discountfactorsJson = mtmJson.getJSONArray("discountvalues");

        double[] legfractions = new double[legfractionsJson.length()];
        for (int i = 0 ; i < legfractions.length ; i++) {
            legfractions[i] = legfractionsJson.getDouble(i);
        }

        double[] discountfactors = new double[discountfactorsJson.length()];
        for (int i = 0 ; i < discountfactors.length ; i++) {
            discountfactors[i] = discountfactorsJson.getDouble(i);
        }

        return makeExposureStrFromJava(tradeid, curvename, counterparty, exposures, legfractions, discountfactors);
    }

    /**
     * <p>Turn some fields into JSON.
     * </p>
     *
     * @param tradeid A String
     * @param curvename A String
     * @param counterparty A String
     * @param netCvaExposure Double
     * @param spreadrates Array of doubles
     * @param hazardrates Array of doubles
     * @param defaultprob Array of doubles
     * @param cvaexposurebyleg Array of doubles
     * @return A string which can be directly turned into JSON
     */
    public static String makeTradeExposureStrFromJava(String tradeid, String curvename, String counterparty,
            double netCvaExposure, double[] spreadrates, double[] hazardrates, double[] defaultprob,
            double[] cvaexposurebyleg) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{");
        stringBuilder.append(" \"tradeid\": \"" + tradeid + "\"");
        stringBuilder.append(", \"curvename\": \"" + curvename + "\"");
        stringBuilder.append(", \"counterparty\": \"" + counterparty + "\"");
        stringBuilder.append(", \"cva\": " + netCvaExposure);

        stringBuilder.append(", \"spreadrates\": [");
        for (int i = 0 ; i < spreadrates.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(spreadrates[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(", \"hazardrates\": [");
        for (int i = 0 ; i < hazardrates.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(hazardrates[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(", \"defaultprob\": [");
        for (int i = 0 ; i < defaultprob.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(defaultprob[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(", \"cvaexposurebyleg\": [");
        for (int i = 0 ; i < cvaexposurebyleg.length; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(cvaexposurebyleg[i]);
        }
        stringBuilder.append("]");

        stringBuilder.append(" }");
        return stringBuilder.toString();
    }


    /**
     * <p>Escape <em>escaped</em> double quotes in strings.
     * </p>
     *
     * @param s Any old string
     * @return
     */
    public static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

    /**
     * <p>Construct and configure the gRPC connection, see
     * <a href="https://github.com/grpc/proposal/blob/master/A6-client-retries.md#retry-policy-capabilities">
     * retry-policy-capabilities</a> and
     * <a href=""
     * </p>
     * <p>TODO Make these parameterisable</p>
     *
     * @param host A load balancer DNS name
     * @param port Load balancer port
     * @return
     */
    public static ManagedChannelBuilder<?> getManagedChannelBuilder(String host, int port) {

        //XXX
        System.out.printf("@@@ ManagedChannelBuilder BEFORE '%s' %d%n", host, port);
        //XXX ANY IP CAUSES THE HANG.
        //host = "1.2.3.4";
        //XXX INTERNAL
        //XXX EXTERNAL http://34.140.255.126:30001/
        //
        host = "34.78.109.213";
        System.out.printf("@@@ ManagedChannelBuilder UPDATE '%s' %d%n", host, port);
        ManagedChannelBuilder<?> managedChannelBuilder =
                //ManagedChannelBuilder.forAddress(host, port);
                // Use Netty directly for "withOption"
                NettyChannelBuilder
                .forAddress(host, port)
                .withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, TEN_THOUSAND);

        // No SSL, only a demo.
        managedChannelBuilder.usePlaintext();

        // May retries for each RPC
        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", Double.valueOf(FIVE_ATTEMPTS));
        retryPolicy.put("maxAttempts", Double.valueOf(1));
        retryPolicy.put("initialBackoff", "0.2s");
        retryPolicy.put("maxBackoff", "10s");
        retryPolicy.put("backoffMultiplier", Double.valueOf(2));
        retryPolicy.put("retryableStatusCodes", List.of("RESOURCE_EXHAUSTED"));

        Map<String, Object> methodConfig = new HashMap<>();
        Map<String, Object> name = new HashMap<>();
        name.put("service", "com_hazelcast_platform_demos_banking_cva.JetToCpp");
        name.put("method", "streamingCall");
        // Fail if cannot connect, otherwise will hang
        name.put("timeout", "10s");

        methodConfig.put("name", List.of(name));
        //methodConfig.put("retryPolicy", retryPolicy);
        //XXX
        System.out.println("DONT USE " + retryPolicy);

        Map<String, Object> serviceConfig = new HashMap<>();
        serviceConfig.put("methodConfig", List.of(methodConfig));

        managedChannelBuilder.defaultServiceConfig(serviceConfig);
        System.out.println("DO USE " + serviceConfig);
        managedChannelBuilder.keepAliveTime(1, TimeUnit.SECONDS);

        // Deactivates stats
        //XXX managedChannelBuilder.enableRetry();
        managedChannelBuilder.disableRetry();

        // Don't use - May not be fully implemented. Max retries for all RPCs
        //managedChannelBuilder.maxRetryAttempts(3);

        hack(host, port);

        return managedChannelBuilder;
    }

    /**
     * FIXME Test connectivity
     *
     * @param host
     * @param port
     */
    @SuppressFBWarnings({"REC_CATCH_EXCEPTION", "DM_DEFAULT_ENCODING"})
    static void hack(String host, int port) {
        try {
            //XXX
            System.out.println("BEFORE SOCKET " + host + " " + port);
            Socket socket = new Socket();
            System.out.println("TIMEOUT WAS " + socket.getSoTimeout());
            socket.setSoTimeout(TEN_THOUSAND);
            SocketAddress endpoint = new InetSocketAddress(host, port);
            System.out.println("BEFORE CONNECT TO " + endpoint);
            socket.connect(endpoint, TEN_THOUSAND);
            System.out.println("AFTER CONNECT TO " + endpoint);
            System.out.println("AFTER SOCKET " + Objects.toString(socket));
            System.out.println("AFTER SOCKET bound " + socket.isBound());
            System.out.println("AFTER SOCKET connected " + socket.isConnected());
            System.out.println("AFTER SOCKET closed " + socket.isClosed());
            System.out.println("BEFORE WRITE A");
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("BEFORE WRITE B");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("BEFORE WRITE C");
            printWriter.println("hello");
            System.out.println("BEFORE WRITE D");
            String world = bufferedReader.readLine();
            System.out.println("BEFORE WRITE E");
            System.out.println("GOT '" + Objects.toString(world) + "'");
            System.out.println("BEFORE WRITE F");
            printWriter.close();
            System.out.println("BEFORE WRITE F");
            bufferedReader.close();
            System.out.println("BEFORE SOCKET CLOSE");
            socket.close();
            System.out.println("AFTER SOCKET CLOSE");
        } catch (Exception eee) {
            System.out.println("MY EXCEPTION");
            eee.printStackTrace(System.out);
        }

    }
}
