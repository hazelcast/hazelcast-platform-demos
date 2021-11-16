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

package hazelcast.platform.demos.industry.iiot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.hazelcast.org.codehaus.commons.nullanalysis.NotNull;

import lombok.Data;

/**
 * <p>Required properties for the application, with the
 * prefix of "{@code my}".
 * </p>
 * <p>Fields in this class are marked as "{@code @NotNull}" so will fail
 * fast if omitted, and are logged by their setters.
 * </p>
 */
@ConfigurationProperties("my")
@Validated
@Data
public class MyProperties {

    @NotNull
    private String buildTimestamp;
    @NotNull
    private String buildUserName;
    @NotNull
    private String mongoCollection1;
    @NotNull
    private String mongoDatabase;
    @NotNull
    private String mongoPassword;
    @NotNull
    private String mongoUsername;
    @NotNull
    private String project;

}
