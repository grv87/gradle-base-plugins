#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project Gradle plugin
 * for buildSrc project
 * Copyright © 2018  Basil Peace
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
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/**
 * Specification for {@link org.fidata.gradle.ProjectPlugin} class
 * for buildSrc project
 */
class ProjectPluginBuildSrcSpecification extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = File.createTempDir('compatTest', '-project')

  File buildFile = new File(testProjectDir, 'build.gradle')

  final File buildSrcTestProjectDir = new File(testProjectDir, 'buildSrc')

  File buildSrcBuildFile = new File(buildSrcTestProjectDir, 'build.gradle')

  File buildSrcPropertiesFile = new File(buildSrcTestProjectDir, 'gradle.properties')

  static final Map<String, String> EXTRA_PROPERTIES = [
    'artifactoryUser'    : System.getProperty('org.fidata.compatTest.artifactoryUser'),
    'artifactoryPassword': System.getProperty('org.fidata.compatTest.artifactoryPassword'),
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
    initEmptyGitRepository(testProjectDir)

    assert buildFile.createNewFile()

    assert buildSrcTestProjectDir.mkdir()
    buildSrcBuildFile << '''\
      plugins {
        id 'org.fidata.project'
      }
    '''.stripIndent()

    buildSrcPropertiesFile.withPrintWriter { PrintWriter printWriter ->
      EXTRA_PROPERTIES.each { String key, String value ->
        printWriter.println "$key=$value"
      }
    }
  }

  // run after every feature method
  void cleanup() {
    /*
     * WORKAROUND:
     * Jenkins doesn't set CI environment variable
     * https://issues.jenkins-ci.org/browse/JENKINS-36707
     * <grv87 2018-06-27>
     */
    if (success || System.getenv().with { containsKey('CI') || containsKey('JENKINS_URL') }) {
      testProjectDir.deleteDir()
    }
  }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void 'can be applied to buildSrc project'() {
    given: 'buildSrc does\'t have its own gradle.properties'

    when: 'Gradle task is run for main project'
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments('--full-stacktrace')
      .withPluginClasspath()
      .forwardOutput()
      .build()

    then: 'no exception is thrown'
    noExceptionThrown()

    (success = true) != null
  }

  // helper methods
}
