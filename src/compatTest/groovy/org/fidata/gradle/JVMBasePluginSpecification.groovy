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
 * Specification for {@link org.fidata.gradle.JVMBasePlugin} class
 */
class JVMBasePluginSpecification extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = File.createTempDir('compatTest', '-project')

  File buildFile = new File(testProjectDir, 'build.gradle')
  File propertiesFile = new File(testProjectDir, 'gradle.properties')

  static final Map<String, String> EXTRA_PROPERTIES = [
    'artifactoryUser'    : 'dummyArtifactoryUser',
    'artifactoryPassword': 'dummyArtifactoryPassword',
    'gitUsername': 'dummyGitUser',
    'gitPassword': 'dummyGitPassword',
    'ghToken': 'dummyGhToken',
    'gpgKeyId'            : 'ABCD1234',
  ]

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  void setup() {
    initEmptyGitRepository(testProjectDir)

    buildFile << '''\
      plugins {
        id 'org.fidata.base.jvm'
      }
    '''.stripIndent()

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
      testProjectDir.deleteDir()
    }
  }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods

  void 'sets Java encoding to UTF-8'() {
    given:
    buildFile << '''\
      sourceSets.main.java.srcDirs = ['src']
      apply plugin: 'java'
    '''
    and:
    File srcDir = new File(testProjectDir, 'src')
    srcDir.mkdir()
    new File(srcDir, 'EncodingTest.java').bytes = """\
      public final class EncodingTest {
        public static final String utf8String() {
          return "${ Character.toChars(codepoint) }";
        }
      }
    """.stripIndent().getBytes('UTF-8')

    when:
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments('compileJava')
      .withPluginClasspath()
      .build()

    then:
    ClassLoader cl = new URLClassLoader(new File(testProjectDir, 'build/classes/java/main').toURI().toURL())
    Class c = cl.loadClass('EncodingTest')
    String result = c.getMethod('utf8String').invoke(null)
    result.length() == 2
    and:
    result.codePointAt(0) == codepoint

    where:
    codepoint = 0x1D54B // U+1D54B Double-Struck Capital T
  }

  void 'copies license file into resources META-INF directory'() {
    given: 'license file'
    new File(testProjectDir, 'LICENSE').text = 'Dummy license file'

    when:
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments('processResources')
      .withPluginClasspath()
      .build()

    then: 'resources META-INF directory contains license file'
    new File(testProjectDir, 'build/resources/main/META-INF/LICENSE').text == 'Dummy license file' // TODO

    (success = true) != null
  }

  // helper methods
}
