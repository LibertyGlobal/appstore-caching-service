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
package com.lgi.appstore.cache.test.framework.container;

import com.lgi.appstore.cache.test.framework.steps.WiremockStep;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.List;

import static java.lang.String.format;

public class AppStoreCachingContainer extends GenericContainer<AppStoreCachingContainer> {
    public static final int NGINX_PORT = 8080;
    private static final int HEALTH_CHECK_PORT = 8080;

    private static final String DOCKER_IMAGE_TAG = "daccloud/appstore-caching-service:latest";
    private static final String BUNDLE_CONTAINER_DIRECTORY = "/data/nginx";
    private static final String HOST_DOMAIN_FROM_CONTAINER = "host.testcontainers.internal";

    public AppStoreCachingContainer(WiremockStep wiremock, File directory) {
        super(DockerImageName.parse(DOCKER_IMAGE_TAG).asCompatibleSubstituteFor("nginx"));

        final List<String> environmentVariables = List.of(
                format("ASBS_SERVICE=%s", getASBSServiceUrl(wiremock)),
                format("ENCRYPTED_BUNDLES_PATH=%s", BUNDLE_CONTAINER_DIRECTORY)
        );

        addExposedPorts(NGINX_PORT, HEALTH_CHECK_PORT);
        Testcontainers.exposeHostPorts(wiremock.port());

        setEnv(environmentVariables);
        setHostAccessible(true);
        withAccessToHost(true);
        withImagePullPolicy(PullPolicy.alwaysPull());
        withFileSystemBind(directory.getAbsolutePath(), BUNDLE_CONTAINER_DIRECTORY, BindMode.READ_ONLY);
        waitingFor(Wait.forListeningPort());
    }

    private String getASBSServiceUrl(WiremockStep wiremock) {
        return format("%s:%s", HOST_DOMAIN_FROM_CONTAINER, wiremock.port());
    }
}
