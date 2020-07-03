plugins {
    id("gradlebuild.distribution.packaging")
}

dependencies {
    coreRuntimeOnly(platform(project(":corePlatform")))

    pluginsRuntimeOnly(platform(project(":distributionsBasics")))

    pluginsRuntimeOnly(project(":signing"))
}
