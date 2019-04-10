#!/usr/bin/env groovy
/*
 * GradleRunnerUtils class
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
package org.fidata.testfixtures.gradle

import groovy.transform.CompileStatic
import org.gradle.api.internal.tasks.TaskExecutionOutcome

/**
 * Utils for {@link org.gradle.testkit.runner.GradleRunner}
 */
@CompileStatic
final class GradleRunnerUtils {
  /**
   * WORKAROUND:
   * No tasks are returned in {@link org.gradle.testkit.runner.BuildResult#taskPaths} dry-run mode.
   * https://github.com/gradle/gradle/issues/2732
   * When this issue is fixed, <code>.taskPaths(TaskExecutionOutcome.SKIPPED)</code> should be used directly
   * @param output Gradle dry-run output
   * @return list of skipped task names
   * <grv87 2018-06-22>
   */
  static List<String> skippedTaskPathsGradleBugWorkaround(String output) {
    output.readLines().findAll { it.endsWith(" ${ TaskExecutionOutcome.SKIPPED.message }") }.collect { it[0..it.lastIndexOf(' ') - 1] }
  }

  // Suppress default constructor for noninstantiability
  private GradleRunnerUtils() {
    throw new UnsupportedOperationException()
  }
}
