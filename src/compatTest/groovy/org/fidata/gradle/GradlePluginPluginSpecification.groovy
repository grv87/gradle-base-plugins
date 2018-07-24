#!/usr/bin/env groovy
/*
 * Specification for org.fidata.plugin Gradle plugin
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

import static org.fidata.gradle.utils.GradleRunnerUtils.skippedTaskPathsGradleBugWorkaround
import spock.lang.Specification
import spock.lang.Unroll
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult
import java.nio.file.Files
import org.apache.commons.io.FileUtils

/**
 * Specification for {@link org.fidata.gradle.GradlePluginPlugin} class
 */
class GradlePluginPluginSpecification extends Specification {
  // fields
  boolean success = false

  final File testProjectDir = Files.createTempDirectory('compatTest').toFile()

  File buildFile = new File(testProjectDir, 'build.gradle')
  File settingsFile = new File(testProjectDir, 'settings.gradle')
  File propertiesFile = new File(testProjectDir, 'gradle.properties')

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

    buildFile << '''\
      plugins {
        id 'org.fidata.plugin'
      }
    '''.stripIndent()

    settingsFile << '''\
      enableFeaturePreview('STABLE_PUBLISHING')
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
      FileUtils.deleteDirectory(testProjectDir)
    }
  }

  // run after the last feature method
  // void cleanupSpec() { }

  // feature methods
  /*
   * WORKAROUND:
   * This can't be tested in functional tests
   * since `org.fidata.plugin` requires `enableFeaturePreview('STABLE_PUBLISHING')` in `settings.gradle`.
   * Without that we got error:
   * `org.gradle.api.InvalidUserDataException: Cannot configure the 'publishing' extension after it has been accessed.`
   * Looks like ProjectBuilder doesn't support `settings.gradle`.
   * <grv87 2018-07-01>
   */
  void 'sets project group by default'() {
    given: 'task to print project group'
    buildFile << '''\
      task('printGroup').doLast {
        file('group') << project.group
      }
    '''.stripIndent()

    when: 'task is queried'
    build('printGroup')

    then: 'project group is set'
    new File(testProjectDir, 'group').text == 'org.fidata.gradle'

    (success = true) != null
  }

  void 'doesn\'t set project group when it has been already set'() {
    given: 'task to print project group'
    buildFile << '''\
      task('printGroup').doLast {
        file('group') << project.group
      }
    '''.stripIndent()
    and: 'project group is already set'
    buildFile << '''\
      group = 'org.fidata'
    '''

    when: 'task is queried'
    build('printGroup')

    then: 'project group is not changed'
    new File(testProjectDir, 'group').text == 'org.fidata'

    (success = true) != null
  }

  @Unroll
  void 'adds #taskName task'() {
    when: '#taskName and functionalTest tasks are run'
    BuildResult result = build('--dry-run', taskName, 'functionalTest')

    then: '#taskName is run'
    List<String> output = skippedTaskPathsGradleBugWorkaround(result.output)
    output.contains ':' + taskName
    and: '#taskName is run after functionalTest task'
    output.indexOf(':' + taskName) > output.indexOf(':functionalTest')

    (success = true) != null

    where:
    taskName << ['compatTest', 'gradleTest']
  }

  void 'does not provide mavenJava publication'() {
    given: 'tasks to print lists of publications and tasks'
    buildFile << '''\
      task('listPublications').doLast {
        file('publications').withPrintWriter { PrintWriter printWriter ->
          publishing.publications.each {
            printWriter.println it.name
          }
        }
      }

      task('listTasks').doLast {
        file('tasks').withPrintWriter { PrintWriter printWriter ->
          tasks.each {
            printWriter.println it.name
          }
        }
      }
    '''.stripIndent()
    when: 'task to print list of publications is queried'
    build('listPublications')

    then: 'mavenJava publication is not in the list'
    !new File(testProjectDir, 'publications').text.split().contains('mavenJava')

    when: 'task to print list of tasks is queried'
    build('listTasks')

    then: 'mavenJava task is not in the list'
    !new File(testProjectDir, 'tasks').text.split().contains(taskName)

    (success = true) != null

    where:
    taskName << ['generateMetadataFileForMavenJavaPublication', 'generatePomFileForMavenJavaPublication']
  }

  // helper methods
  protected BuildResult build(String... arguments) {
    GradleRunner.create()
      .withGradleVersion(System.getProperty('compat.gradle.version'))
      .withProjectDir(testProjectDir)
      .withArguments([*arguments, '--stacktrace', '--refresh-dependencies'])
      .withPluginClasspath()
      .build()
  }
}
