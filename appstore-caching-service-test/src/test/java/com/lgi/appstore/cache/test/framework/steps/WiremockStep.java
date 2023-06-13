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
package com.lgi.appstore.cache.test.framework.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class WiremockStep {
    private static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    public int port() {
        return wireMockServer.port();
    }

    public void stub202(String path) {
        wireMockServer.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(202).withHeader("Retry-After", "60s")));
    }

    public void stub202IfXRequestIsSet(String path) {
        wireMockServer.stubFor(get(urlEqualTo(path))
                .withHeader("x-request-id", new AnythingPattern())
                .willReturn(aResponse().withStatus(202).withHeader("Retry-After", "60s")));
    }

    public void stub202IfXRequestEquals(String path, String xRequestId) {
        wireMockServer.stubFor(get(urlEqualTo(path))
                .withHeader("x-request-id", new EqualToPattern(xRequestId))
                .willReturn(aResponse().withStatus(202).withHeader("Retry-After", "60s")));
    }

    public void stubMalformed(String path, Fault fault) {
        wireMockServer.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withFault(fault)));
    }

    public boolean noInteractions(String path) {
        return wireMockServer.countRequestsMatching(get(urlEqualTo(path)).build().getRequest()).getCount() == 0;
    }

    void start() {
        wireMockServer.start();
    }

    void stop() {
        wireMockServer.stop();
    }
}
