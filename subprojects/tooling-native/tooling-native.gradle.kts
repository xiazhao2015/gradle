/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    id("gradlebuild.distribution.api-java")
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":core"))
    implementation(project(":fileCollections"))
    implementation(project(":platformBase"))
    implementation(project(":platformNative"))
    implementation(project(":languageNative"))
    implementation(project(":testingNative"))
    implementation(project(":toolingApi"))
    implementation(project(":ide")) {
        because("To pick up various builders (which should live somewhere else)")
    }

    implementation(library("guava"))

    testImplementation(testFixtures(project(":platformNative")))

    crossVersionTestDistributionRuntimeOnly(project(":distributionsNative"))
}
