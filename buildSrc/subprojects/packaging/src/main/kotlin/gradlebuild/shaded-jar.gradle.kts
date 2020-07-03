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

package gradlebuild

import gradlebuild.basics.classanalysis.Attributes
import gradlebuild.shade.ArtifactTypes.buildReceiptType
import gradlebuild.shade.ArtifactTypes.classTreesType
import gradlebuild.shade.ArtifactTypes.entryPointsType
import gradlebuild.shade.ArtifactTypes.manifestsType
import gradlebuild.shade.ArtifactTypes.relocatedClassesAndAnalysisType
import gradlebuild.shade.ArtifactTypes.relocatedClassesType
import gradlebuild.shade.extension.ShadedJarExtension
import gradlebuild.shade.tasks.ShadedJar
import gradlebuild.shade.transforms.FindBuildReceipt
import gradlebuild.shade.transforms.FindClassTrees
import gradlebuild.shade.transforms.FindEntryPoints
import gradlebuild.shade.transforms.FindManifests
import gradlebuild.shade.transforms.FindRelocatedClasses
import gradlebuild.shade.transforms.ShadeClasses


plugins {
    id("gradlebuild.module-identity")
}

val shadedJarExtension = extensions.create<ShadedJarExtension>("shadedJar", createConfigurationToShade())

registerTransforms(shadedJarExtension)

val shadedJarTask = addShadedJarTask(shadedJarExtension)

addInstallShadedJarTask(shadedJarTask)
addShadedJarVariant(shadedJarTask)
configureShadedSourcesJarVariant()

fun Project.registerTransforms(shadedJarExtension: ShadedJarExtension) {
    afterEvaluate {
        dependencies {
            registerTransform(ShadeClasses::class) {
                from
                    .attribute(Attributes.artifactType, "jar")
                    .attribute(Attributes.minified, true)
                to.attribute(Attributes.artifactType, relocatedClassesAndAnalysisType)
                parameters {
                    shadowPackage = "org.gradle.internal.impldep"
                    keepPackages = shadedJarExtension.keepPackages.get()
                    unshadedPackages = shadedJarExtension.unshadedPackages.get()
                    ignoredPackages = shadedJarExtension.ignoredPackages.get()
                }
            }
        }
    }
    dependencies {
        registerTransform(FindRelocatedClasses::class) {
            from.attribute(Attributes.artifactType, relocatedClassesAndAnalysisType)
            to.attribute(Attributes.artifactType, relocatedClassesType)
        }
        registerTransform(FindEntryPoints::class) {
            from.attribute(Attributes.artifactType, relocatedClassesAndAnalysisType)
            to.attribute(Attributes.artifactType, entryPointsType)
        }
        registerTransform(FindClassTrees::class) {
            from.attribute(Attributes.artifactType, relocatedClassesAndAnalysisType)
            to.attribute(Attributes.artifactType, classTreesType)
        }
        registerTransform(FindManifests::class) {
            from.attribute(Attributes.artifactType, relocatedClassesAndAnalysisType)
            to.attribute(Attributes.artifactType, manifestsType)
        }
        registerTransform(FindBuildReceipt::class) {
            from.attribute(Attributes.artifactType, relocatedClassesAndAnalysisType)
            to.attribute(Attributes.artifactType, buildReceiptType)
        }
    }
}

fun createConfigurationToShade() = jvm.createResolvableConfiguration("jarsToShade") {
        attributes {
            library(LibraryElements.JAR)
            withExternalDependencies()
        }
    }.apply {
        withDependencies {
            this.add(project.dependencies.create(project))
        }
    }

fun addShadedJarTask(shadedJarExtension: ShadedJarExtension): TaskProvider<ShadedJar> {
    val configurationToShade = shadedJarExtension.shadedConfiguration

    return tasks.register("${project.name}ShadedJar", ShadedJar::class) {
        jarFile.set(layout.buildDirectory.file(provider { "shaded-jar/${moduleIdentity.baseName.get()}-shaded-${moduleIdentity.version.get().baseVersion.version}.jar" }))
        classTreesConfiguration.from(configurationToShade.artifactViewForType(classTreesType))
        entryPointsConfiguration.from(configurationToShade.artifactViewForType(entryPointsType))
        relocatedClassesConfiguration.from(configurationToShade.artifactViewForType(relocatedClassesType))
        manifests.from(configurationToShade.artifactViewForType(manifestsType))
        buildReceiptFile.from(configurationToShade.artifactViewForType(buildReceiptType))
    }
}

fun addInstallShadedJarTask(shadedJarTask: TaskProvider<ShadedJar>) {
    val installPathProperty = "${project.name}ShadedJarInstallPath"
    fun targetFile(): File {
        val file = findProperty(installPathProperty)?.let { File(findProperty(installPathProperty) as String) }

        if (true == file?.isAbsolute) {
            return file
        } else {
            throw IllegalArgumentException("Property $installPathProperty is required and must be absolute!")
        }
    }
    tasks.register<Copy>("install${project.name.capitalize()}ShadedJar") {
        from(shadedJarTask.map { it.jarFile })
        into(provider { targetFile().parentFile })
        rename { targetFile().name }
    }
}

fun addShadedJarVariant(shadedJarTask: TaskProvider<ShadedJar>) {
    val implementation by configurations
    val shadedImplementation by configurations.creating { // TODO want to create a bucket here!
        isCanBeResolved = false
        isCanBeConsumed = false
    }
    implementation.extendsFrom(shadedImplementation)

    val shadedRuntimeElements = jvm.createOutgoingElements("shadedRuntimeElements") {
        attributes {
            library(LibraryElements.JAR)
            withShadowedDependencies()
            withTargetJvmVersion(8)
        }
        extendsFrom(shadedImplementation)
        addArtifact(shadedJarTask) // TODO have to test if this is actually enough
    }

    // publish only the shaded variant
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.addVariantsFromConfiguration(shadedRuntimeElements) { }
    javaComponent.withVariantsFromConfiguration(configurations["runtimeElements"]) {
        skip()
    }
    javaComponent.withVariantsFromConfiguration(configurations["apiElements"]) {
        skip()
    }
}

fun configureShadedSourcesJarVariant() {
    val implementation by configurations
    val sourcesPath = jvm.createResolvableConfiguration("sourcesPath") {
        extendsFrom(implementation)
        attributes {
            documentation("gradle-source-folders")
        }
    }
    tasks.named<Jar>("sourcesJar") {
        from(sourcesPath.incoming.artifactView { lenient(true) }.files)
    }
    val sourcesElements by configurations
    jvm.utilities.configureAttributes(sourcesElements) {
        withShadowedDependencies()
    }
}

fun Configuration.artifactViewForType(artifactTypeName: String) = incoming.artifactView {
    attributes.attribute(Attributes.artifactType, artifactTypeName)
}.files
