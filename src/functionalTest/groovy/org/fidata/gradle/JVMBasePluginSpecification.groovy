#!/usr/bin/env groovy
/*
 * Specification for org.fidata.base.jvm Gradle plugin
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
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Specification for {@link JVMBasePlugin} class
 */
class JVMBasePluginSpecification extends Specification {
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
    EXTRA_PROPERTIES.each { String key, String value ->
      project.ext.setProperty key, value
    }
  }

  // run after every feature method
  // void cleanup() { }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void 'adds functionalTest task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.base.jvm'

    then: 'functionalTest task exists'
    Task functionalTest = project.tasks.getByName('functionalTest')

    when: 'project is evaluated'
    project.evaluate()

    then: 'functionalTest should be run after test task'
    functionalTest.shouldRunAfter.getDependencies(functionalTest).contains(project.tasks.getByName('test'))
  }

  void 'provides mavenJava publication'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.base.jvm'
    and: 'project evaluated'
    project.evaluate()

    then: 'mavenJava publication exists'
    project.publishing.publications.getByName('mavenJava')
    and: 'mavenJava task exists'
    project.tasks.getByName(taskName)

    where:
    taskName << ['generateMetadataFileForMavenJavaPublication', 'generatePomFileForMavenJavaPublication']
  }

  // helper methods
}
