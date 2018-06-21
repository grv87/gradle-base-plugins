/*
 * ResignGitCommit task class
 * Copyright © 2017  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Amend previous git commit adding sign to it ("resign" commit)
 *
 * This is necessary since JGit doesn't support signed commits yet.
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=382212
 */
public class ResignGitCommit extends DefaultTask {
  /**
   * Resigns previous git commit
   */
  @TaskAction
  void resign() {
    project.exec {
      commandLine 'git', 'commit', '--amend', '--no-edit', "--gpg-sign=${ project.property('gpgKeyId') }"
    }
  }
}
