#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project.groovy Gradle plugin
 * Copyright © 2017-2018  Basil Peace
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

import static org.fidata.gradle.utils.GradleRunnerUtils.skippedTaskPathsGradleBugWorkaround
import spock.lang.Specification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult

/**
 * Specification for {@link GroovyProjectPlugin} class
 */
class GroovyProjectPluginCompatTest extends Specification {
  // fields

  @Rule
  TemporaryFolder testProjectDir = new TemporaryFolder()

  File buildFile

  File propertiesFile

  // fixture methods

  // run before every feature method
  void setup() {
    buildFile = testProjectDir.newFile('build.gradle')
    propertiesFile = testProjectDir.newFile('gradle.properties')
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

  void "configures check task dependencies"() {
    given:
    'build file without any extra configuration'
    buildFile << '''
      plugins {
          id 'org.fidata.project.groovy'
      }
    '''

    when:
    'check task is run'
    BuildResult result = build('--dry-run', 'check')

    then:
    'codenarcTest task is also run'
    and: 'codenarcFunctionalTest task is also run'
    List<String> output = skippedTaskPathsGradleBugWorkaround(result.output)
    /*output.any { it == ':codenarcTest' }
    output.any { it == ':codenarcFunctionalTest' }*/
    output.contains ':codenarcTest'
    output.contains ':codenarcFunctionalTest'
    /*output.with {
      any { it == ':codenarcTest' }
      any { it == ':codenarcFunctionalTest' }
      // any { it == ':jacoco' } TODO
    }*/
  }

  void "configures release task dependencies"() {
    given:
    'build file without any extra configuration'
    buildFile << '''
      plugins {
          id 'org.fidata.project.groovy'
      }
    '''

    when:
    'release task is run'
    BuildResult result = build('--dry-run', 'release')

    then:
    'functionalTest task is also run'
    List<String> output = skippedTaskPathsGradleBugWorkaround(result.output)
    // output.any { it == ':functionalTest' }
    output.contains ':functionalTest'
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
}
