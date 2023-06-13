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
package com.lgi.appstore.cache.test.mocked;

import com.lgi.appstore.cache.test.framework.steps.AppStoreCachingStep;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class NginxITCase extends AppStoreCachingStep {

    @Test
    void shouldForwardRequestToBackendWhenFileIsAbsent() {
        // GIVEN
        stub202InAppStoreBundleService("bundle-111.zip");

        // WHEN
        final var bundle = getBundle("bundle-111.zip");

        // THEN
        verifyResponseCode(bundle, HttpStatus.SC_ACCEPTED);
        verifyHadInteractions("bundle-111.zip");
    }

    @Test
    void shouldDownloadBundleIfPresent() throws IOException {
        // GIVEN
        storeBundleOnNFS("bundle-123.zip", "hello world");

        // WHEN
        final var bundle = getBundle("bundle-123.zip");

        // THEN
        verifyResponseCode(bundle, HttpStatus.SC_OK);
        verifyResponseContent(bundle, "hello world");
        verifyNoInteractions("bundle-123.zip");
    }

    @Test
    void shouldGenerateXRequestIdInNginx() {
        // GIVEN
        stub202InAppStoreBundleServiceIfXRequestIsSet("bundle-111.zip");

        // WHEN
        final var bundle = getBundle("bundle-111.zip");

        // THEN
        verifyResponseCode(bundle, HttpStatus.SC_ACCEPTED);
    }

    @Test
    void shouldTakeXRequestIdFromClient() {
        // GIVEN
        stub202InAppStoreBundleServiceIfXRequestEquals("bundle-111.zip", "my-id");

        // WHEN
        final var bundle = getBundle("bundle-111.zip", "my-id");

        // THEN
        verifyResponseCode(bundle, HttpStatus.SC_ACCEPTED);
    }

    @Test
    void shouldReturnInternalServerErrorWhenASBSIsUnreachable() {
        // GIVEN
        stubConnectionResetByPeer("bundle-111.zip");

        // WHEN
        final var bundle = getBundle("bundle-111.zip");

        // THEN
        verifyResponseCode(bundle, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldHaveAccessToReadBundlesOnNFS() throws Exception {
        verifyNginxUserBelongToGroup("nginx-bundles");
        verifyNginxBundlesGroupHasId("1000");
    }
}