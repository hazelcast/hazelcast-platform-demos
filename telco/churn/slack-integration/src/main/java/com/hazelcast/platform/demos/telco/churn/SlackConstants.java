/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

/**
 * <p>The constants are for interacting with Slack. Because we cannot
 * really vary them, keep them separate from {@link MyConstants}.
 * </p>
 * <p>See <a href="https://api.slack.com/methods/chat.postMessage">Slack Documentation</a>.
 * </p>
 */
public class SlackConstants {

    public static final String PARAM_CHANNEL = "channel";
    public static final String PARAM_TEXT = "text";
    public static final String PARAM_TOKEN = "token";

    public static final String RESPONSE_BOT_ID = "bot_id";
    public static final String RESPONSE_KEY_OK = "ok";
    public static final String RESPONSE_MESSAGES = "messages";
    public static final String RESPONSE_TEXT = "text";
    public static final String RESPONSE_TIMESTAMP = "ts";

    public static final String READ_MESSAGE_URL = "https://slack.com/api/conversations.history";
    public static final String WRITE_MESSAGE_URL = "https://slack.com/api/chat.postMessage";

}
