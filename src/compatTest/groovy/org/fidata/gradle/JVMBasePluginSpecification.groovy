#!/usr/bin/env groovy
/*
 * Specification for org.fidata.base.jvm Gradle plugin
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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.api.Project
import spock.lang.Specification
import java.nio.file.Files

/**
 * Specification for {@link org.fidata.gradle.JVMBasePlugin} class
 */
class JVMBasePluginSpecification extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = Files.createTempDirectory('compatTest').toFile()

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
    ].each { it.execute(null, testProjectDir).waitFor() }

    File buildFile = new File(testProjectDir, 'build.gradle')
    buildFile << '''
      plugins {
        id 'org.fidata.base.jvm'
      }
    '''

    File propertiesFile = new File(testProjectDir, 'gradle.properties')
    propertiesFile.withPrintWriter { PrintWriter printWriter ->
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
      testProjectDir.delete()
    }
  }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void 'copies license file into resources META-INF directory'() {
    given:
    'license file'
    new File(testProjectDir, 'LICENSE').text = 'Dummy license file'

    when:
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments('processResources')
      .withPluginClasspath()
      .build()

    then:
    'resources META-INF directory contains license file'
    new File(testProjectDir, 'build/resources/main/META-INF/LICENSE').text == 'Dummy license file' // TODO

    (success = true) != null
  }

  // helper methods
}
