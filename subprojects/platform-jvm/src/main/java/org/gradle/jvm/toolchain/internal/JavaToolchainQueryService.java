/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.jvm.toolchain.internal;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.file.FileFactory;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.toolchain.JavaInstallationRegistry;

import javax.inject.Inject;
import java.io.File;

public class JavaToolchainQueryService {

    private final SharedJavaInstallationRegistry registry;
    private final JavaInstallationRegistry installationRegistry;
    private final FileFactory fileFactory;

    @Inject
    public JavaToolchainQueryService(SharedJavaInstallationRegistry registry, JavaInstallationRegistry installationRegistry, FileFactory fileFactory) {
        this.registry = registry;
        this.installationRegistry = installationRegistry;
        this.fileFactory = fileFactory;
    }

    // TODO: replace with actual query methods
    public Provider<JavaToolchain> getOne() {
        return new DefaultProvider<>(() -> queryOne());
    }

    private JavaToolchain queryOne() {
        return registry.listInstallations().stream().findFirst().map(this::asToolchain).orElseThrow(() -> new InvalidUserDataException("No java installations defined"));
    }

    private JavaToolchain asToolchain(File javaHome) {
        return new JavaToolchain(javaHome, installationRegistry, fileFactory);
    }
}
