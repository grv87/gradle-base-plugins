#!/usr/bin/env groovy
/*
 * Specification for org.fidata.project Gradle plugin
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
import com.google.common.collect.ImmutableMap
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Specification for {@link ProjectPlugin} class
 */
class ProjectPluginSpecification extends Specification {
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
    'gpgKeyId'            : 'ABCD1234',
  ])

  // fixture methods

  // run before the first feature method
  // void setupSpec() { }

  // run before every feature method
  void setup() {
    initEmptyGitRepository(testProjectDir)

    buildFile << '''\
      plugins {
        id 'org.fidata.project'
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

  void 'can generate changelog'() {
    given: 'some git history'
    [
      ['git', 'commit', '--message', 'feat: cool feature', '--allow-empty', '--no-gpg-sign'],
      ['git', 'commit', '--message', 'feat: another super feature', '--allow-empty', '--no-gpg-sign'],
      ['git', 'commit', '--message', 'fix: use cool library instead of trying to reinvent the wheel', '--allow-empty', '--no-gpg-sign'],
    ].each { List<String> it -> it.execute((List)null, testProjectDir).waitFor() }

    when: 'generateChangelog task is run'
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments('generateChangelog', '--full-stacktrace')
      .withPluginClasspath()
      .build()

    then: 'CHANGELOG.md is generated inside build/changelog directory'
    new File(testProjectDir, 'build/changelog/CHANGELOG.md').exists()

    when: 'generateChangelogTxt task is run'
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments('generateChangelogTxt', '--full-stacktrace')
      .withPluginClasspath()
      .forwardOutput()
      .build()

    then: 'CHANGELOG.txt is generated inside build/changelog directory'
    new File(testProjectDir, 'build/changelog/CHANGELOG.txt').exists()

    (success = true) != null
  }

  @Unroll
  void '#verb a new version when we are #masterBranchAdverb and shouldRelease property #shouldReleaseState'() {
    given: 'clean repository on #branch branch'
    /*
     * WORKAROUND:
     * gitPublishCommit and gitPublishPush tasks don't have correct properties if gitPublishReset was not run.
     * See https://github.com/ajoberstar/gradle-git-publish/issues/60
     * <grv87 2018-10-27>
     */
    buildFile << """\
      file(${ versionFileName.inspect() }).text = version

      import org.ajoberstar.grgit.Grgit

      afterEvaluate {
        tasks*.enabled = false

        Grgit dummyGrgit = Grgit.init(dir: file('build/gitPublish'))
        tasks.gitPublishCommit.grgit = dummyGrgit
        tasks.gitPublishPush.grgit = dummyGrgit
      }
    """.stripIndent()
    [
      ['git', 'checkout', '-b', branch],
      ['git', 'add', buildFile.name],
      ['git', 'commit', '--message', 'feat: initial version', '--no-gpg-sign'],
    ].each { List<String> it -> it.execute((List)null, testProjectDir).waitFor() }

    when: 'shouldRelease property #shouldReleaseState and release task is run'
    List<String> gradleArguments = ['release', '--info', '--full-stacktrace', '--offline']
    if (shouldRelease != null) {
      gradleArguments << "-PshouldRelease=$shouldRelease".toString()
    }
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments(gradleArguments)
      .withPluginClasspath()
      .forwardOutput()
      .build()

    then: 'version is #expectedVersion'
    new File(testProjectDir, versionFileName).text == expectedVersion

    where:
    versionFileName = 'VERSION'

    branch | shouldRelease | expectedVersion
    'master' | true  | '1.0.0'
    'master' | false | '1.0.0-SNAPSHOT'
    'master' | null  | '1.0.0-SNAPSHOT'
    'feature/super-cool' | true  | '1.0.0-super-cool-SNAPSHOT'
    'feature/super-cool' | false | '1.0.0-super-cool-SNAPSHOT'
    'feature/super-cool' | null  | '1.0.0-super-cool-SNAPSHOT'

    verb = expectedVersion.endsWith('-SNAPSHOT') ? 'doesn\' release' : 'releases'
    masterBranchAdverb = branch == 'master' ? 'on the master branch' : 'not on the master branch'
    shouldReleaseState = shouldRelease == null ? 'is not provided' : "is set to $shouldRelease"
  }

  // helper methods
}
