#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project Gradle plugin
 * for extra properties
 * Copyright © 2018  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package org.fidata.gradle

import static org.fidata.testfixtures.TestFixtures.initEmptyGitRepository
import com.google.common.collect.ImmutableMap
import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Specification for {@link ProjectPlugin} class
 * for extra properties
 */
class ProjectPluginExtraPropertiesSpecification extends Specification {
  // fields
  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()

  Project project

  static final Map<String, String> EXTRA_PROPERTIES = ImmutableMap.copyOf([
    'artifactoryUser'    : 'dummyArtifactoryUser',
    'artifactoryPassword': 'dummyArtifactoryPassword',
    'gitUsername': 'dummyGitUser',
    'gitPassword': 'dummyGitPassword',
    'ghToken': 'dummyGhToken',
    'gpgKeyId': 'ABCD1234',
  ])

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  void setup() {
    initEmptyGitRepository(testProjectDir.root)
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

    when: 'plugin is being applied'
    project.apply plugin: 'org.fidata.project'

    then: 'no exception is thrown'
    noExceptionThrown()

    when: 'project is being evaluated'
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
    and: 'project is being evaluated'
    project.evaluate()

    then: 'PluginApplicationException is thrown'
    thrown(PluginApplicationException)

    where:
    property << EXTRA_PROPERTIES.keySet()
  }

  // helper methods
}
