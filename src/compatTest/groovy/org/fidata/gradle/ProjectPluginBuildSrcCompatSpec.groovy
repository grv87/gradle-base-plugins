#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project Gradle plugin
 * for buildSrc project
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
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/**
 * Specification for {@link ProjectPlugin} class
 * for buildSrc project
 */
class ProjectPluginBuildSrcCompatSpec extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = File.createTempDir('compatTest', '-project')

  File buildFile = new File(testProjectDir, 'build.gradle')

  final File buildSrcTestProjectDir = new File(testProjectDir, 'buildSrc')

  File buildSrcBuildFile = new File(buildSrcTestProjectDir, 'build.gradle')

  File buildSrcPropertiesFile = new File(buildSrcTestProjectDir, 'gradle.properties')

  static final Map<String, String> EXTRA_PROPERTIES = ImmutableMap.copyOf([
    'artifactoryUser'    : System.getProperty('org.fidata.compatTest.artifactoryUser'),
    'artifactoryPassword': System.getProperty('org.fidata.compatTest.artifactoryPassword'),
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
    initEmptyGitRepository(testProjectDir)

    assert buildFile.createNewFile()

    assert buildSrcTestProjectDir.mkdir()
    buildSrcBuildFile << '''\
      plugins {
        id 'org.fidata.project'
      }

      afterEvaluate {
        assert tasks.findByName('codenarcBuildSrc') == null
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

    when: 'Gradle task is being run for main project'
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
