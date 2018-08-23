#!/usr/bin/env groovy
/*
 * ReportPathDirectorException class
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

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Exception thrown by {@link PathDirector} when it is unable to determine path for reports for some task
 */
class ReportPathDirectorException extends RuntimeException {
  private ReportPathDirectorException(Object task, Throwable throwable) {
    super(sprintf('Unable to determine path for reports for task %s', [task]), throwable)
  }

  /**
   * Default constructor
   * @param object Provider of task for which path should have been determined
   * @param throwable Original exception
   */
  ReportPathDirectorException(TaskProvider<? extends Task> taskProvider, Throwable throwable) {
    this((Object)taskProvider, throwable)
  }

  /**
   * Default constructor
   * @param object Task for which path should have been determined
   * @param throwable Original exception
   */
  ReportPathDirectorException(Task task, Throwable throwable) {
    this((Object)task, throwable)
  }
}
