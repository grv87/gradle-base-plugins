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
    [
      ['git', 'init'],
      ['git', 'commit', '--message', 'Initial commit', '--allow-empty'],
    ].each { List<String> it -> it.execute((List)null, dir).waitFor() }
  }

  // Suppress default constructor for noninstantiability
  private TestFixtures() {
    throw new UnsupportedOperationException()
  }
}
