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
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/**
 * Specification for {@link JvmBasePlugin} class
 */
class JvmBasePluginCompatSpec extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = File.createTempDir('compatTest', '-project')

  File buildFile = new File(testProjectDir, 'build.gradle')
  File propertiesFile = new File(testProjectDir, 'gradle.properties')

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
      .withArguments('compileJava', '-Dfile.encoding=Windows-1251', '--full-stacktrace')
      .withPluginClasspath()
      .forwardOutput()
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
    String license = 'LICENSE'
    new File(testProjectDir, license).text = 'Dummy license file'

    when:
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments('processResources', '--full-stacktrace')
      .withPluginClasspath()
      .forwardOutput()
      .build()

    then: 'resources META-INF directory contains license file'
    new File(testProjectDir, "build/resources/main/META-INF/$license").text == 'Dummy license file' // TODO

    (success = true) != null
  }

  // helper methods
}
