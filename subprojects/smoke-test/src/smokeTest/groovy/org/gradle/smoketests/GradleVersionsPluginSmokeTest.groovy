/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.smoketests

import spock.lang.Ignore

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Ignore("Until this plugin is updated to be compliant with 5.x (causes deadlocks)")
class GradleVersionsPluginSmokeTest extends AbstractSmokeTest {
    def 'can check for updated versions'() {
        given:
        buildFile << """
            plugins {
                id "com.github.ben-manes.versions" version "$TestedVersions.gradleVersions"
            }

            subprojects {
                apply plugin: 'java'
                
                ${jcenterRepository()}
            }
            project(":sub1") {
                dependencies {
                    compile group: 'log4j', name: 'log4j', version: '1.2.14'
                }
            }
            project(":sub2") {
                dependencies {
                    compile group: 'junit', name: 'junit', version: '4.10'
                }
            }
        """
        settingsFile << """
            include "sub1", "sub2"
        """

        when:
        def result = runner('dependencyUpdates', '-DoutputFormatter=txt').forwardOutput().build()

        then:
        result.task(':dependencyUpdates').outcome == SUCCESS
        result.output.contains("- junit:junit [4.10 -> 4.12]")
        result.output.contains("- log4j:log4j [1.2.14 -> 1.2.17]")
        
        file("build/dependencyUpdates/report.txt").exists()
    }
}
