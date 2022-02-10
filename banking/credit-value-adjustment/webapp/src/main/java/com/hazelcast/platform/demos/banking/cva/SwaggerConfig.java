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

package com.hazelcast.platform.demos.banking.cva;

import org.springframework.context.annotation.Configuration;

/**
 * <p>Optional, activate SwaggerUI on
 * <a href="http://localhost:8080/swagger-ui.html#/">/swagger-ui.html#/</a>
 * </p>
 */
@Configuration
public class SwaggerConfig {

    static {
        // TODO: Until this is fixed https://github.com/springfox/springfox/issues/3462
        // TODO: Also needs fragments.html
        System.setProperty("spring.mvc.pathmatch.matching-strategy", "ant-path-matcher");
    }

    /**
     * <p>Use <a href="https://swagger.io/">Swagger</a> for easier
     * interaction for testing REST.
     * </p>
     * TODO: Needs https://github.com/springfox/springfox/issues/3462
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
          .select()
          .apis(RequestHandlerSelectors.any())
          .paths(PathSelectors.any())
          .build();
    }*/

}
