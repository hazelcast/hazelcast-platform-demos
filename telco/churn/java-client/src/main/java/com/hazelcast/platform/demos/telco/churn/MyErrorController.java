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

package com.hazelcast.platform.demos.telco.churn;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>An override for the standard error controller to pick up
 * faults in a cleaner manner.
 * </p>
 * TODO This should log the original URL
 */
@Controller
public class MyErrorController implements ErrorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyErrorController.class);

    private static final String ERROR_PATH = "error";

    /**
     * <p>Indicate the "<i>path</i>" to redirect errors to.
     * This is the path that the error "{@code @RequestMapping}" handles.
     * </p>
     */
    @Override
    public String getErrorPath() {
        return "/" + ERROR_PATH;
    }

    /**
     * <p>Handle an error, mainly logging and apologising.
     * </p>
     *
     * TODO This returns "{@code @ResponseBody}" so the web-page to use, rather than
     * a view. It would be better if this can bounce to Node.js.
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @param httpSession
     * @return
     */
    @RequestMapping("/" + ERROR_PATH)
    @ResponseBody
    public String error(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, HttpSession httpSession) {

        String path = httpServletRequest.getRequestURL().toString();
        int statusCode = httpServletResponse.getStatus();

        // This app hasn't faulted, it could be user typed in a bad URL.
        LOGGER.warn("error(): {} for {}", statusCode, path);

        return "<html><body>An error has occurred, details are in the logs</body></html>";
    }
}
