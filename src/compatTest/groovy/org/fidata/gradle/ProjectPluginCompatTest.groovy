#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project Gradle plugin
 * Copyright © 2017  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle

import spock.lang.Specification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult

/**
 * Specification for {@link ProjectPlugin} class
 */
class ProjectPluginCompatTest extends Specification {
  // fields

  @Rule
  TemporaryFolder testProjectDir = new TemporaryFolder()

  File buildFile = testProjectDir.newFile('build.gradle')

  File propertiesFile = testProjectDir.newFile('gradle.properties')

  // fixture methods

  // run before every feature method
  void setup() {
    /*
     * BLOCKED:
     * https://github.com/tschulte/gradle-semantic-release-plugin/issues/24
     * https://github.com/tschulte/gradle-semantic-release-plugin/issues/25
     */
    [
      'git init',
      'git commit --message "Initial commit" --allow-empty',
    ].each { it.execute(null, testProjectDir.root).waitFor() }
  }

  // run after every feature method
  void cleanup() {
    testProjectDir.delete()
  }

  // run before the first feature method
  // void setupSpec() { }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void "provides codenarcBuildSrc task"() {
    given:
    'build file without any extra configuration'
    buildFile << '''
      plugins {
          id 'org.fidata.project'
      }
    '''

    when:
    'codenarcBuildSrc task is run'
    BuildResult result = build('--dry-run', 'codenarcBuildSrc')

    then:
    'it succeeds'
    List<String> output = skippedTaskPathsGradleBugWorkaround(result.output)
    output.contains ':codenarcBuildSrc'
    // output.any { it == ':codenarcBuildSrc' }
  }

  void "configures check task dependencies"() {
    given:
    'build file without any extra configuration'
    buildFile << '''
      plugins {
          id 'org.fidata.project'
      }
    '''

    when:
    'check task is run'
    BuildResult result = build('--dry-run', 'check')

    then:
    'codenarcBuildSrc task is also run'
    List<String> output = skippedTaskPathsGradleBugWorkaround(result.output)
    output.contains ':codenarcBuildSrc'
    // output.any { it == ':codenarcBuildSrc' }
    // output.any { it == ':jacoco' } TODO
  }

  void "configures release task dependencies"() {
    given:
    'build file without any extra configuration'
    buildFile << '''
      plugins {
          id 'org.fidata.project'
      }
    '''

    when:
    'release task is run'
    BuildResult result = build('--dry-run', 'release')

    then:
    'check task is also run'
    and: 'test task is also run'
    List<String> output = skippedTaskPathsGradleBugWorkaround(result.output)
    /*output.any { it == ':build' }
    output.any { it == ':check' }*/
    output.contains ':build'
    output.contains ':check'
    /*output.with {
       any { taskName -> taskName == ':check' }
       any { taskName -> taskName == ':test' }
    }*/
  }

  // helper methods

  protected BuildResult build(String... arguments) {
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir.root)
      .withArguments([*arguments, '--stacktrace', '--refresh-dependencies'])
      .withPluginClasspath()
      .build()
  }

  protected List<String> skippedTaskPathsGradleBugWorkaround(String output) {
    //Due to https://github.com/gradle/gradle/issues/2732 no tasks are returned in dry-run mode. When fixed ".taskPaths(SKIPPED)" should be used directly
    output.readLines().findAll { it.endsWith(' SKIPPED') }.collect { it[0..it.lastIndexOf(' ')] }
  }
}
