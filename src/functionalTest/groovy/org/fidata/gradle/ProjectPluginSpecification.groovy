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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle

import static org.fidata.testfixtures.TestFixtures.initEmptyGitRepository
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
  final TemporaryFolder testProjectDir = new TemporaryFolder()

  Project project

  static final Map<String, String> EXTRA_PROPERTIES = [
    'artifactoryUser'    : 'dummyArtifactoryUser',
    'artifactoryPassword': 'dummyArtifactoryPassword',
    'gitUsername': 'dummyGitUser',
    'gitPassword': 'dummyGitPassword',
    'ghToken': 'dummyGhToken',
    'gpgKeyId': 'ABCD1234',
  ]

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

  void 'provides lifecycle tasks'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'clean task exists'
    project.tasks.findByName('clean')
    and: 'build task exists'
    Task build = project.tasks.getByName('build')
    and: 'check task exists'
    Task check = project.tasks.getByName('check')
    and: 'release task exists'
    Task release = project.tasks.getByName('release')

    when: 'project evaluated'
    project.evaluate()

    then: 'release task depends on build task'
    release.taskDependencies.getDependencies(release).contains(build)
    and: 'release task depends on check task'
    release.taskDependencies.getDependencies(release).contains(check)
    and: 'build task does not depend on check task'
    !build.taskDependencies.getDependencies(build).contains(check)
  }

  void 'provides prerequisites lifecycle tasks'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'task exists'
    project.tasks.getByName(task)

    where:
    task << [
      'installPrerequisites', 'updatePrerequisites', 'outdatedPrerequisites',
      'installBuildTools',    'updateBuildTools',    'outdatedBuildTools',
      'installDependencies',  'updateDependencies',  'outdatedDependencies',
    ]
  }

  void 'provides link task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'lint task exists'
    Task lint = project.tasks.getByName('lint')

    when: 'project evaluated'
    project.evaluate()

    then: 'check task depends on lint task'
    Task check = project.tasks['check']
    check.taskDependencies.getDependencies(check).contains(lint)
  }

  void 'provides codenarc task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'codenarc task exists'
    project.tasks.getByName('codenarc')
  }

  void 'provides codenarcBuildSrc task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'codenarcBuildSrc task exists'
    Task codenarcBuildSrc = project.tasks.getByName('codenarcBuildSrc')
    and: 'codenarcBuildSrc task is an instance of CodeNarc'
    CodeNarc.isInstance(codenarcBuildSrc)

    when: 'project evaluated'
    project.evaluate()

    then: 'codenarc task depends on codenarcBuildSrc task'
    Task codenarc = project.tasks['codenarc']
    codenarc.taskDependencies.getDependencies(codenarc).contains(codenarcBuildSrc)
    and: 'check task does not depend on codenarcBuildSrc task'
    Task check = project.tasks['check']
    !check.taskDependencies.getDependencies(check).contains(codenarcBuildSrc)
  }

  @Unroll
  void '#testDescription #filename to codenarcBuildSrc task source'() {
    given: 'file exists'
    File file = new File(testProjectDir.root, filename)
    file.parentFile.mkdirs()
    file.createNewFile()

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then:
    project.tasks['codenarcBuildSrc'].source.files.contains(file) == include

    where:
    filename                                   | include
    'build.gradle'                             | true
    'settings.gradle'                          | true
    'some-script.gradle'                       | true
    'gradle.properties'                        | false
    'gradle/file1.gradle'                      | true
    'gradle/file2.txt'                         | false
    'gradle/file3.groovy'                      | true
    'build/file1.gradle'                       | false
    'build/file1.groovy'                       | false
    'buildSrc/build.gradle'                    | true
    'buildSrc/settings.gradle'                 | true
    'buildSrc/gradle/file4.gradle'             | true
    'buildSrc/build/file5.gradle'              | false
    'buildSrc/src/file6.groovy'                | true
    'buildSrc/buildSrc/build.gradle'           | true
    'buildSrc/buildSrc/settings.gradle'        | true
    'buildSrc/buildSrc/src/file7.groovy'       | true
    'buildSrc/buildSrc/build/file8.gradle'     | false
    'buildSrc/buildSrc/src/build/file9.groovy' | true
    'config/dir1/file10.groovy'                | true
    'src/test.groovy'                          | false
    'src/resources/test.groovy'                | false
    'src/resources/test.gradle'                | false
    'Jenkinsfile'                              | true
    testDescription = include ? 'Adds' : 'Doesn\'t add'
  }

  void 'provides reportsDir read-only properties'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'property exists'
    Object propertyValue = project.property(property)
    and: 'property value is an instance of File'
    File.isInstance(propertyValue)

    when: 'property is tried to be set'
    project.setProperty(property, project.file('dummyFile'))

    then: 'GroovyRuntimeException is thrown'
    thrown(GroovyRuntimeException)

    where:
    property << ['reportsDir', 'htmlReportsDir', 'xmlReportsDir', 'jsonReportsDir', 'txtReportsDir']
  }

  void 'provides taskTree task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'taskTree task exists'
    project.tasks.getByName('taskTree')
  }

  void 'sets project group by default'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'project group is set'
    project.group.toString() == 'org.fidata'

    when: 'project evaluated'
    project.evaluate()

    then: 'project group is set'
    project.group.toString() == 'org.fidata'
  }

  void 'doesn\'t set project group when it has been already set'() {
    given: 'project group is set'
    String group = 'com.example'
    project.group = group

    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'project group is not changed'
    project.group.toString() == group

    when: 'project evaluated'
    project.evaluate()

    then: 'project group is not changed'
    project.group.toString() == group
  }

  void 'provides license property'() {
    given: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    when: 'license property set with valid SPDX license identifier'
    project.license == 'Apache-2.0'

    then: 'no exception is thrown'
    noExceptionThrown()

    when: 'license property set empty'
    project.license == ''

    then: 'no exception is thrown'
    noExceptionThrown()

    when: 'license property set null'
    project.license == null

    then: 'no exception is thrown'
    noExceptionThrown()

    when: 'license property set with invalid SPDX license identifier'
    project.license = 'Apache2.0'

    then:
    thrown InvalidLicenseStringException
  }

  void 'provides tags property'() {
    given: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    when: 'tags are set'
    project.tags.set(['test', 'compatTest'])

    then: 'no exception is thrown'
    noExceptionThrown()
  }

  void 'provides contacts extension'() {
    given: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    when: 'contacts extension is available'
    project.contacts {
      'test@example.com' {
        moniker 'Tester'
      }
    }

    then: 'no exception is thrown'
    noExceptionThrown()
  }

  // helper methods
}
