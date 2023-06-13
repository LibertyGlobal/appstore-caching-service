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

import com.github.tomakehurst.wiremock.http.Fault;
import com.lgi.appstore.cache.test.framework.container.AppStoreCachingContainer;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.shaded.com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lgi.appstore.cache.test.framework.container.AppStoreCachingContainer.NGINX_PORT;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.assertj.core.api.Assertions.assertThat;

public class AppStoreCachingStep {
    private static final int EXIT_CODE_SUCCESS = 0;
    private static final int ENT_PATTERN_GROUP_ID_IDX = 3;
    private static final Pattern GET_ENT_OUTPUT_PATTERN = Pattern.compile("(.*):(.*):(.*):(.*)");

    private static File tempDirectory = Files.createTempDir();
    private static WiremockStep appstoreBundleServiceWiremock;
    private static AppStoreCachingContainer appStoreCachingContainer;

    public String getBaseUri() {
        return format("http://%s:%s", appStoreCachingContainer.getContainerIpAddress(), appStoreCachingContainer.getMappedPort(NGINX_PORT));
    }

    @Step("Store bundle on NFS (name: '{name}', content: '{content}')")
    public void storeBundleOnNFS(String name, String content) throws IOException {
        final var newFile = new File(tempDirectory, name);
        FileUtils.write(newFile, content, Charset.defaultCharset());
    }

    @Step("Mock response from AppStore-Bundle-Service (name: '{name}')")
    public void stub202InAppStoreBundleService(String name) {
        appstoreBundleServiceWiremock.stub202(format("/applications/%s", name));
    }

    @Step("Mock response from AppStore-Bundle-Service (name: '{name}', x-request-id is set)")
    public void stub202InAppStoreBundleServiceIfXRequestIsSet(String name) {
        appstoreBundleServiceWiremock.stub202IfXRequestIsSet(format("/applications/%s", name));
    }

    @Step("Mock response from AppStore-Bundle-Service (name: '{name}', x-request-id: {xRequestId})")
    public void stub202InAppStoreBundleServiceIfXRequestEquals(String name, String xRequestId) {
        appstoreBundleServiceWiremock.stub202IfXRequestEquals(format("/applications/%s", name), xRequestId);
    }

    @Step("Get a bundle (name: '{bundle}') with HTTP request")
    public Response getBundle(String bundle) {
        return RestAssured.given()
                .baseUri(getBaseUri())
                .get("/{bundle}", bundle)
                .then().extract().response();
    }

    @Step("Get a bundle (name: '{bundle}') with HTTP request (x-request-id: {xRequestId}")
    public Response getBundle(String bundle, String xRequestId) {
        return RestAssured.given()
                .baseUri(getBaseUri())
                .header("x-request-id", xRequestId)
                .get("/{bundle}", bundle)
                .then().extract().response();
    }

    @Step("Check the code of HTTP response (expected: {expectedCode})")
    public void verifyResponseCode(Response response, int expectedCode) {
        assertThat(response.getStatusCode()).isEqualTo(expectedCode);
    }

    @Step("Check the HTTP content (expected: {expectedValue})")
    public void verifyResponseContent(Response response, String expectedValue) {
        assertThat(response.getBody().prettyPrint()).isEqualTo(expectedValue);
    }

    @Step("Check there were no interactions with AppStore-Bundle-Service")
    public void verifyNoInteractions(String name) {
        assertThat(appstoreBundleServiceWiremock.noInteractions(format("/applications/%s", name))).isTrue();
    }

    @Step("Check there was interaction with AppStore-Bundle-Service")
    public void verifyHadInteractions(String name) {
        assertThat(appstoreBundleServiceWiremock.noInteractions(format("/applications/%s", name))).isFalse();
    }

    @Step("Mock malformed response from AppStore-Bundle-Service (fault: 'ConnectionResetByPeer')")
    public void stubConnectionResetByPeer(String name) {
        appstoreBundleServiceWiremock.stubMalformed(format("/applications/%s", name), Fault.CONNECTION_RESET_BY_PEER);
    }

    @Step("Check that nginx-user belongs to a group (name: '{group}')")
    public void verifyNginxUserBelongToGroup(String group) throws IOException, InterruptedException {
        assertThat(fetchGroupsForUser("nginx")).contains(group);
    }

    @Step("Check that nginx-bundles has id (id: '{id}')")
    public void verifyNginxBundlesGroupHasId(String id) throws IOException, InterruptedException {
        assertThat(getGroupId("nginx-bundles")).isNotNull().isEqualTo(id);
    }

    @Step("Get user groups from container (user: '{user}')")
    private Collection<String> fetchGroupsForUser(String user) throws IOException, InterruptedException {
        final var execResult = appStoreCachingContainer.execInContainer("/usr/bin/id", "-G", "-n", user);

        if (execResult.getExitCode() == EXIT_CODE_SUCCESS) {
            return Arrays.stream(execResult.getStdout().trim().split(" "))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Step("Get group id from container (group: '{group}')")
    private String getGroupId(String group) throws InterruptedException, IOException {
        final var execResult = appStoreCachingContainer.execInContainer("/usr/bin/getent", "group", group);

        if (execResult.getExitCode() == EXIT_CODE_SUCCESS) {
            final var matcher = GET_ENT_OUTPUT_PATTERN.matcher(execResult.getStdout().trim());
            if (matcher.matches()) {
                return matcher.group(ENT_PATTERN_GROUP_ID_IDX);
            }
        }
        return null;
    }

    @BeforeAll
    public static void start() {
        appstoreBundleServiceWiremock = new WiremockStep();
        appstoreBundleServiceWiremock.start();
        appStoreCachingContainer = new AppStoreCachingContainer(appstoreBundleServiceWiremock, tempDirectory);
        appStoreCachingContainer.start();
    }

    @AfterAll
    public static void stop() {
        appStoreCachingContainer.stop();
        appstoreBundleServiceWiremock.stop();
        deleteQuietly(tempDirectory);
    }
}