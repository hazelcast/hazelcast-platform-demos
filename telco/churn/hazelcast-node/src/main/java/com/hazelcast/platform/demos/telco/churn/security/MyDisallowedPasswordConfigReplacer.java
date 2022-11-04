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

package com.hazelcast.platform.demos.telco.churn.security;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.replacer.spi.ConfigReplacer;
import com.hazelcast.platform.demos.telco.churn.MyUtils;

/**
 * <p>Provide a way to hide values in config files. In config files hold the
 * encoded value, and this class provides the decoded value.
 * </p>
 * <p>Hazelcast provides some readymade ones, such
 * as {@link com.hazelcast.config.replacer.EncryptionReplacer}.
 * </p>
 */
public class MyDisallowedPasswordConfigReplacer implements ConfigReplacer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyDisallowedPasswordConfigReplacer.class);

    /**
     * <p>Properties to replace will look like "{@code $CHURN{...}}".
     * </p>
     */
    @Override
    public String getPrefix() {
        return "CHURN";
    }

    /**
     * <p>Provide the replacement for the input. For proper use would use some
     * sort of encryption algorithm.
     * </p>
     */
    @Override
    public String getReplacement(String arg0) {
        String replacement = MyUtils.rot13(arg0);
        LOGGER.debug("getReplacament(): '{}' -> '{}'", arg0, replacement);
        return replacement;
    }

    /**
     * <p>Allow configuration, currently ignored.
     * </p>
     */
    @Override
    public void init(Properties arg0) {
        LOGGER.trace("init({}", arg0);
    }

}
