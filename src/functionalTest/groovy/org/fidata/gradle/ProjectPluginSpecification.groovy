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
import com.google.common.collect.ImmutableMap
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
 * Specification for {@link ProjectPlugin} class
 */
class ProjectPluginSpecification extends Specification {
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

  void 'provides lifecycle tasks'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'clean task exists'
    project.tasks.findByName('clean')
    and: 'assemble task exists'
    Task assemble = project.tasks.getByName('assemble')
    and: 'check task exists'
    Task check = project.tasks.getByName('check')
    and: 'release task exists'
    Task release = project.tasks.getByName('release')

    when: 'project evaluated'
    project.evaluate()

    then: 'release task depends on build task'
    release.taskDependencies.getDependencies(release).contains(assemble)
    and: 'release task depends on check task'
    release.taskDependencies.getDependencies(release).contains(check)
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

    and: 'check task depends on lint task'
    Task check = project.tasks.getByName('check')
    check.taskDependencies.getDependencies(check).contains(lint)
  }

  void 'provides codenarc task'() {
    when: 'plugin is applied'
    project.apply plugin: 'org.fidata.project'

    then: 'codenarc task exists'
    Task codenarc = project.tasks.getByName('codenarc')

    and: 'lint task depends on codenarc task'
    Task lint = project.tasks.getByName('lint')
    lint.taskDependencies.getDependencies(lint).contains(codenarc)

    when: 'groovy plugin is applied'
    project.apply plugin: 'groovy'

    then: 'codenarc task depends on codenarcMain task'
    Task codenarcMain = project.tasks.getByName('codenarcMain')
    codenarc.taskDependencies.getDependencies(codenarc).contains(codenarcMain)
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
    Task codenarc = project.tasks.getByName('codenarc')
    codenarc.taskDependencies.getDependencies(codenarc).contains(codenarcBuildSrc)
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
    project.tasks.getByName('codenarcBuildSrc').source.files.contains(file) == include

    where:
    filename                                   | include
    'build.gradle'                             | Boolean.TRUE
    'settings.gradle'                          | Boolean.TRUE
    'some-script.gradle'                       | Boolean.TRUE
    'gradle.properties'                        | Boolean.FALSE
    'gradle/file1.gradle'                      | Boolean.TRUE
    'gradle/file2.txt'                         | Boolean.FALSE
    'gradle/file3.groovy'                      | Boolean.TRUE
    'build/file1.gradle'                       | Boolean.FALSE
    'build/file1.groovy'                       | Boolean.FALSE
    'buildSrc/build.gradle'                    | Boolean.TRUE
    'buildSrc/settings.gradle'                 | Boolean.TRUE
    'buildSrc/gradle/file4.gradle'             | Boolean.TRUE
    'buildSrc/build/file5.gradle'              | Boolean.FALSE
    'buildSrc/src/file6.groovy'                | Boolean.TRUE
    'buildSrc/buildSrc/build.gradle'           | Boolean.TRUE
    'buildSrc/buildSrc/settings.gradle'        | Boolean.TRUE
    'buildSrc/buildSrc/src/file7.groovy'       | Boolean.TRUE
    'buildSrc/buildSrc/build/file8.gradle'     | Boolean.FALSE
    'buildSrc/buildSrc/src/build/file9.groovy' | Boolean.TRUE
    'config/dir1/file10.groovy'                | Boolean.TRUE
    'src/test.groovy'                          | Boolean.FALSE
    'src/resources/test.groovy'                | Boolean.FALSE
    'src/resources/test.gradle'                | Boolean.FALSE
    'Jenkinsfile'                              | Boolean.TRUE
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
