#!/usr/bin/env groovy
/*
 * Specification for org.fidata.plugin Gradle plugin
 * Copyright ©  Basil Peace
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
import org.gradle.api.publish.PublishingExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Specification for {@link GradlePluginPlugin} class
 */
class GradlePluginPluginFunctionalSpecification extends Specification {
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
  void 'sets project group by default'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.plugin'

    then: 'project group is set'
    project.group.toString() == 'org.fidata.gradle'

    when: 'project evaluated'
    project.evaluate()

    then: 'project group is set'
    project.group.toString() == 'org.fidata.gradle'
  }

  void 'doesn\'t set project group when it has been already set'() {
    given: 'project group is set'
    String group = 'com.example'
    project.group = group

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.plugin'

    then: 'project group is not changed'
    project.group.toString() == group

    when: 'project evaluated'
    project.evaluate()

    then: 'project group is not changed'
    project.group.toString() == group
  }

  @Unroll
  void 'provides #taskName task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.plugin'

    then: '#taskName task exists'
    Task task = project.tasks.getByName(taskName)

    when: 'project evaluated'
    project.evaluate()

    then: '#taskName task should run after functionalTest task'
    Task functionalTest = project.tasks.getByName('functionalTest')
    task.shouldRunAfter.getDependencies(task).contains(functionalTest)

    where:
    taskName << ['compatTest', 'gradleTest']
  }

  void 'does not provide mavenJava publication'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.plugin'

    and: 'project evaluated'
    project.evaluate()

    then: 'mavenJava publication doesn\'t exist'
    !project.extensions.getByType(PublishingExtension).publications.findByName('mavenJava')

    and: '#taskName task doesn\'t exist'
    !project.tasks.findByName(taskName)

    where:
    taskName << ['generateMetadataFileForMavenJavaPublication', 'generatePomFileForMavenJavaPublication']
  }
}
