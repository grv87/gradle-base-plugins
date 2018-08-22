#!/usr/bin/env groovy
/*
 * GradleRunnerUtils class
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
package org.fidata.gradle.utils

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
  GradleRunnerUtils() {
    throw new UnsupportedOperationException()
  }
}
