/*
 * If not stated otherwise in this file or this component's LICENSE file the
 * following copyright and licenses apply:
 *
 * Copyright 2023 Liberty Global Technology Services BV
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
package com.lgi.appstore.cache.test.real;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NginxTest {
    final String SERVICE_URL = System.getenv("service.url");

    @Test
    void shouldDownloadJsonFromRealService() {
        assumeTrue(isNotEmpty(SERVICE_URL), "Service url is not set");

        // GIVEN
        final var bundle = getBundle("test-id/eos2008c-debug.json");

        // THEN
        assertThat(bundle.getStatusCode()).isEqualTo(200);
        assertThat(bundle.getContentType()).isEqualTo("application/json");
    }

    public Response getBundle(String bundle) {
        return RestAssured.given()
                .baseUri(SERVICE_URL)
                .get(bundle)
                .then().extract().response();
    }
}