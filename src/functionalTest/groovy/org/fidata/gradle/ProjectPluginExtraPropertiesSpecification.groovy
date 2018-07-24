#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project Gradle plugin
 * for extra properties
 * Copyright © 2018  Basil Peace
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

import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Specification for {@link org.fidata.gradle.ProjectPlugin} class
 * for extra properties
 */
class ProjectPluginExtraPropertiesSpecification extends Specification {
  // fields
  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()

  Project project

  static final Map<String, String> EXTRA_PROPERTIES = [
    'artifactoryUser'    : 'dummyArtifactoryUser',
    'artifactoryPassword': 'dummyArtifactoryPassword',
    'gitUsername': 'dummyGitUser',
    'gitPassword': 'dummyGitPassword',
    'ghToken': 'dummyGhToken',
    'gpgKeyId'            : 'ABCD1234',
    'gpgKeyPassword'      : '',
    'gpgSecretKeyRingFile': 'dummyGPGSecretKeyRingFile',
  ]

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  void setup() {
    /*
     * WORKAROUND:
     * https://github.com/tschulte/gradle-semantic-release-plugin/issues/24
     * https://github.com/tschulte/gradle-semantic-release-plugin/issues/25
     * <grv87 2018-06-24>
     */
    [
      'git init',
      'git commit --message "Initial commit" --allow-empty',
    ].each { it.execute(null, testProjectDir.root).waitFor() }
    project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
  }

  // run after every feature method
  // void cleanup() { }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods
  void 'works when all extra properties are set'() {
    given: 'all properties are set'
    EXTRA_PROPERTIES.each { String key, String value ->
      project.ext.setProperty key, value
    }

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'no exception is thrown'
    noExceptionThrown()

    when: 'project evaluated'
    project.evaluate()

    then: 'no exception is thrown'
    noExceptionThrown()
  }

  @Unroll
  void 'requires extra property #property'() {
    given: 'all properties except #property are set'
    EXTRA_PROPERTIES.findAll { key, value -> key != property }.each { String key, String value ->
      project.ext.setProperty key, value
    }

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'
    and: 'project evaluated'
    project.evaluate()

    then: 'PluginApplicationException is thrown'
    thrown(PluginApplicationException)

    where:
    property << EXTRA_PROPERTIES.keySet()
  }

  // helper methods
}
