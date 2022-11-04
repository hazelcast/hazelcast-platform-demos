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

package com.hazelcast.platform.demos.utils;

/**
 * <p>Constants used in utility modules
 * </p>
 */
public class UtilsConstants {

    // Alway run in a container
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final int SQL_RESULT_THRESHOLD = 10;

    // -- Slack
    public static final int HTTP_OK = 200;

    // Our constants, we could change them
    public static final String SLACK_ACCESS_TOKEN = "slack.bot.user.oath.access.token";
    public static final String SLACK_BUILD_USER = "slack.build.user";
    public static final String SLACK_CHANNEL_ID = "slack.bot.channel.id";
    public static final String SLACK_CHANNEL_NAME = "slack.bot.channel.name";
    public static final String SLACK_PROJECT_NAME = "my.project.name";

    // Slack's constant, we can't change them
    public static final String SLACK_PARAM_CHANNEL = "channel";
    public static final String SLACK_PARAM_TEXT = "text";
    public static final String SLACK_RESPONSE_BOT_ID = "bot_id";
    public static final String SLACK_RESPONSE_KEY_OK = "ok";
    public static final String SLACK_RESPONSE_MESSAGES = "messages";
    public static final String SLACK_RESPONSE_TEXT = "text";
    public static final String SLACK_RESPONSE_TIMESTAMP = "ts";
    public static final String SLACK_URL_READ_MESSAGE = "https://slack.com/api/conversations.history";
    public static final String SLACK_URL_WRITE_MESSAGE = "https://slack.com/api/chat.postMessage";

}
