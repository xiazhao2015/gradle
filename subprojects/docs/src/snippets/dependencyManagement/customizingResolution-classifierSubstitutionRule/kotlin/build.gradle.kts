plugins {
    `java-library`
}

allprojects {
    repositories {
        jcenter()
    }
}

// tag::dependencies[]
dependencies {
    implementation("com.google.guava:guava:28.2-jre")
    implementation("co.paralleluniverse:quasar-core:0.8.0")
    implementation(project(":lib"))
}
// end::dependencies[]

// tag::substitution_rule[]
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("co.paralleluniverse:quasar-core"))
            .using(module("co.paralleluniverse:quasar-core:0.8.0"))
            .withoutClassifier()
    }
}
// end::substitution_rule[]

tasks.register("resolve") {
    inputs.files(configurations.runtimeClasspath)
    doLast {
        println(configurations.runtimeClasspath.files.map { it.name })
    }
}
