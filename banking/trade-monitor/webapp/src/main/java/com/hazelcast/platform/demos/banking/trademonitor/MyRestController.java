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

package com.hazelcast.platform.demos.banking.trademonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.javalin.http.Handler;

import org.eclipse.jetty.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.sql.SqlResult;

/**
 * <p>A controller for vending out REST requests, all of which
 * are prefixed by "{@code /rest}". So "{@code /rest/one}",
 * "{@code /rest/two}", "{@code /rest/three}" and so on.
 * </p>
 */
public class MyRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRestController.class);

    private final HazelcastInstance hazelcastInstance;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Hazelcast instance must be shared, not cloned")
    public MyRestController(HazelcastInstance arg0) {
        this.hazelcastInstance = arg0;
    }

    /**
     * <p>Sample handler for testing REST is reachable.
     * </p>
     *
     * @return
     */
    public Handler handleIndex() {
        return context -> {
            LOGGER.info("handleIndex()");
            context.html(String.valueOf(HttpStatus.OK_200));
            context.status(HttpStatus.OK_200);
        };
    }

    /**
     * <p>Woork with a query such as:
     * </p>
     * <pre>
     * http://localhost:8080/rest/sql?query=SELECT%20*%20FROM%20trades
     * </pre>
     *
     * @return
     */
    public Handler handleSql() {
        return context -> {
            String sql = null;
            try {
                sql = context.queryParam("query");
                LOGGER.info("handleSql('{}')", sql);
                if (sql != null && sql.length() > 0) {
                    context.json(this.sql(sql));
                    context.status(HttpStatus.OK_200);
                } else {
                    context.status(HttpStatus.BAD_REQUEST_400);
                }
            } catch (Exception e) {
                LOGGER.error("handleSql('" + sql + "')", e);
                context.status(HttpStatus.BAD_REQUEST_400);
            }
        };
    }


    /**
     * <p>Run an SQL query
     * </p>
     *
     * @param query Already HTML decoded.
     * @return
     */
    public SqlDto sql(String query) {
        try {
            SqlResult sqlResult = this.hazelcastInstance.getSql().execute(query);
            Tuple3<String, String, List<String>> result =
                    MyUtils.prettyPrintSqlResult(sqlResult);

            SqlDto sqlDto = new SqlDto(MyUtils.safeForJsonStr(result.f0()),
                    MyUtils.safeForJsonStr(result.f1()),
                    result.f2());

            LOGGER.trace("sql(query '{}') => '{}'", query, sqlDto);
            return sqlDto;
        } catch (Exception e) {
            LOGGER.error(String.format("sql(query '%s'), Exception: %s", query, e.getMessage()));
            return new SqlDto(MyUtils.safeForJsonStr(e.getMessage()), "", Collections.emptyList());
        }
    }
}
