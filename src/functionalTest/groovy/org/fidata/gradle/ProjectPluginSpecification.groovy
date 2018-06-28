#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project Gradle plugin
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

import org.spdx.spdxspreadsheet.InvalidLicenseStringException
import spock.lang.Specification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Task
import org.gradle.api.plugins.quality.CodeNarc
import spock.lang.Unroll

/**
 * Specification for {@link org.fidata.gradle.ProjectPlugin} class
 */
class ProjectPluginSpecification extends Specification {
  // fields
  @Rule
  TemporaryFolder testProjectDir = new TemporaryFolder()

  Project project

  // fixture methods

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
    project.ext.setProperty('artifactoryUser', 'dummyArtifactoryUser')
    project.ext.setProperty('artifactoryPassword', 'dummyArtifactoryPassword')
    project.ext.setProperty('gitUsername', 'dummyGitUser')
    project.ext.setProperty('gitPassword', 'dummyGitPassword')
    project.ext.setProperty('ghToken', 'dummyGhToken')
    project.ext.setProperty('gpgKeyId', 'ABCD1234')
    project.ext.setProperty('gpgSecretKeyRingFile', 'dummyGPGSecretKeyRingFile')
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

  @Unroll
  void 'provides lifecycle tasks'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    'clean task exists'
    project.tasks.findByName('clean')
    and: 'build task exists'
    Task build = project.tasks.findByName('build')
    and: 'check task exists'
    Task check = project.tasks.findByName('check')
    and: 'release task exists'
    Task release = project.tasks.findByName('release')
    and: 'release task depends on build'
    release.taskDependencies.getDependencies(release).contains(build)
    and: 'release task depends on check'
    release.taskDependencies.getDependencies(release).contains(check)
    and: 'build task does not depend on check'
    !build.taskDependencies.getDependencies(build).contains(check)
  }

  @Unroll
  void 'provides prerequisites lifecycle tasks'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    '#task task exists'
    project.tasks.findByName(task)

    where:
    task << ['prerequisitesInstall', 'prerequisitesUpdate', 'prerequisitesOutdated']
  }

  @Unroll
  void 'provides link task'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    'lint task exists'
    Task lint = project.tasks.findByName('lint')
    and: 'check task depends on lint'
    Task check = project.tasks['check']
    check.taskDependencies.getDependencies(check).contains(lint)
  }

  void 'provides codenarc task'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    'codenarc task exists'
    project.tasks.findByName('codenarc')
  }

  void 'provides codenarcBuildSrc task'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    'codenarcBuildSrc task exists'
    Task codenarcBuildSrc = project.tasks.findByName('codenarcBuildSrc')
    and: 'codenarcBuildSrc task is an instance of CodeNarc'
    CodeNarc.isInstance(codenarcBuildSrc)
    and: 'codenarc task depends on codenarcBuildSrc'
    Task codenarc = project.tasks['codenarc']
    codenarc.taskDependencies.getDependencies(codenarc).contains(codenarcBuildSrc)
    and: 'check task does not depend on codenarcBuildSrc'
    Task check = project.tasks['check']
    !check.taskDependencies.getDependencies(check).contains(codenarcBuildSrc)
  }

  void 'provides reportsDir properties'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    '#property property exists'
    Object propertyValue = project.property(property)
    and: '#property is an instance of File'
    File.isInstance(propertyValue)

    when:
    'property is tried to be set'
    project.setProperty(property, project.file('dummyFile'))

    then:
    'GroovyRuntimeException is thrown'
    thrown(GroovyRuntimeException)

    where:
    property << ['reportsDir', 'htmlReportsDir', 'xmlReportsDir', 'txtReportsDir']
  }

  void 'provides taskTree task'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    'taskTree task exists'
    project.tasks.findByName('taskTree')
  }

  void 'sets project group'() {
    when:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    'project group is set'
    project.group == 'org.fidata'
  }

  void 'provides license property'() {
    given:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    when:
    'license property set with valid SPDX license identifier'
    project.license == 'Apache-2.0'

    then:
    'no exception is thrown'
    noExceptionThrown()

    when:
    'license property set empty'
    project.license == ''

    then:
    'no exception is thrown'
    noExceptionThrown()

    when:
    'license property set null'
    project.license == null

    then:
    'no exception is thrown'
    noExceptionThrown()

    when:
    'license property set with invalid SPDX license identifier'
    project.license = 'Apache2.0'

    then:
    thrown InvalidLicenseStringException
  }

  void 'provides contacts extension'() {
    given:
    'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    when:
    'contacts extension is available'
    project.contacts {
      'test@example.com' {
        moniker 'Tester'
      }
    }

    then:
    'no exception is thrown'
    noExceptionThrown()
  }

  // helper methods
}
