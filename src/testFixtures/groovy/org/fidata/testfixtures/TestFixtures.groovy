#!/usr/bin/env groovy
/*
 * TestFixtures class
 * Copyright ©  Basil Peace
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
      ['git', 'checkout', '-b', 'master'],
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
