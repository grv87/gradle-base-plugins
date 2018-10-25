#!/usr/bin/env groovy
/*
 * TestFixtures class
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
package org.fidata.testfixtures

import groovy.transform.CompileStatic

/**
 * Commonly used test fixtures
 */
@CompileStatic
final class TestFixtures {
  /**
   * WORKAROUND:
   * https://github.com/tschulte/gradle-semantic-release-plugin/issues/24
   * https://github.com/tschulte/gradle-semantic-release-plugin/issues/25
   * <grv87 2018-08-23>
   */
  static void initEmptyGitRepository(File dir) {
    new File(dir, '.gitignore').text = '''\
      # Gradle
      .gradle/
      gradle.properties
      build/
    '''.stripIndent()
    [
      ['git', 'init'],
      ['git', 'add', '.gitignore'],
      ['git', 'commit', '--message', 'feat: initial version', '--no-gpg-sign'],
      /*
       * Without that we got:
       *   java.lang.NullPointerException: Cannot get property 'url' on null object
       *       at de.gliderpilot.gradle.semanticrelease.GithubRepo.memoizedMethodPriv$getMnemo(GithubRepo.groovy:71)
       * Maybe that should be fixed in semantic-release
       */
      ['git', 'remote', 'add', 'origin', 'https://github.com/FIDATA/gradle-base-plugins.compatTest'],
    ].each { List<String> it -> it.execute((List)null, dir).waitFor() }
  }

  // Suppress default constructor for noninstantiability
  private TestFixtures() {
    throw new UnsupportedOperationException()
  }
}
