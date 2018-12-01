/*
 * ResignGitCommit Gradle task class
 * Copyright © 2017-2018  Basil Peace
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
package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import org.fidata.gpg.GpgUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec
import java.nio.file.Paths

/**
 * Amends previous git commit adding sign to it ("resigns" commit)
 *
 * WORKAROUND:
 * This is necessary since JGit doesn't support signed commits yet.
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=382212
 * <grv87 2018-06-22>
 */
@CompileStatic
class ResignGitCommit extends DefaultTask {
  /**
   * Working directory for the task
   * If not provided then project directory is used
   */
  @Optional
  @InputDirectory
  final DirectoryProperty workingDir = newInputDirectory()

  /**
   * Resigns previous git commit
   */
  @TaskAction
  void resign() {
    project.exec { ExecSpec execSpec ->
      execSpec.commandLine 'gpg-agent', '--daemon'
      execSpec.ignoreExitValue = true /* TODO: if it was started then we need not to kill it after
      We better pass password through commandLine than preset - so we need not start agent manually at all */
    }
    try {
      String gpgKeyId = project.rootProject.extensions.extraProperties['gpgKeyId']
      if (project.rootProject.extensions.extraProperties.has('gpgKeyPassphrase')) {
        project.logger.info('ResignGitCommit: presetting gpgKeyPassphrase')
        project.exec { ExecSpec execSpec ->
          execSpec.executable 'gpg-preset-passphrase'
          if (!OperatingSystem.current().windows) {
            execSpec.executable Paths.get('/usr/local/libexec', execSpec.executable).toString()
          }
          execSpec.args '--preset', '--passphrase', project.rootProject.extensions.extraProperties['gpgKeyPassphrase'], GpgUtils.getKeyGrip(project, gpgKeyId)
        }
      }
      project.exec { ExecSpec execSpec ->
        if (workingDir.present) {
          execSpec.workingDir workingDir
        }
        execSpec.commandLine 'git', 'commit', '--amend', '--no-edit', "--gpg-sign=$gpgKeyId"
      }
    } finally {
      project.exec { ExecSpec execSpec ->
        execSpec.commandLine 'gpgconf', '--kill', 'gpg-agent'
      }
    }
  }

  static final org.gradle.api.Namer<TaskProvider<Task>> RESIGN_GIT_COMMIT_TASK_NAMER = new org.gradle.api.Namer<TaskProvider<Task>>() {
    @Override
    String determineName(TaskProvider<Task> commitTaskProvider)  {
      "resign${ commitTaskProvider.name.capitalize() }"
    }
  }

  /**
   * Registers new ResignGitCommit task for specified project using default task name and configuration
   * @param project Project where to register ResignGitCommit task
   * @param commitTaskProvider Provider of original commit task whose result should be resigned. May be in different project than {@code project}
   * @param var3 The action to run to additionally configure ResignGitCommit task
   * @return Provider of ResignGitCommit task
   */
  static TaskProvider<ResignGitCommit> registerTask(Project project, TaskProvider<Task> commitTaskProvider, @DelegatesTo(ResignGitCommit) Closure var3) {
    TaskProvider<ResignGitCommit> resignGitCommitProvider = project.tasks.register(RESIGN_GIT_COMMIT_TASK_NAMER.determineName(commitTaskProvider), ResignGitCommit) { ResignGitCommit resignGitCommit ->
      resignGitCommit.with {
        onlyIf { commitTaskProvider.get().didWork }
        configure var3
      }
    }
    commitTaskProvider.configure { Task commitTask ->
      commitTask.finalizedBy resignGitCommitProvider
    }
    resignGitCommitProvider
  }
}
