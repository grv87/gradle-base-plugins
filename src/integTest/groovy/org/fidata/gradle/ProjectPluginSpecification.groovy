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
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Task
import org.gradle.api.plugins.quality.CodeNarc

/**
 * Specification for {@link org.fidata.project.ProjectPlugin} class
 */
class ProjectPluginSpecification extends Specification {
  // fields
  @Rule
  TemporaryFolder testProjectDir = new TemporaryFolder()

  Project project

  // fixture methods

  // run before every feature method
  void setup() {
    project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
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
    when:
    'plugin is applied'
    // project.apply plugin: 'org.fidata.plugin'
    project.apply plugin: org.fidata.gradle.ProjectPlugin

    then:
    'codenarcBuildSrc task exists'
    Task task = project.tasks.findByName('codenarcBuildSrc')
    and: 'codenarcBuildSrc task is an instance of CodeNarc'
    CodeNarc.class.isInstance(task)
  }

  /*void "configures check task dependencies"() {
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
    output.contains ':build'
    output.contains ':check'
  }*/

  // helper methods

}
