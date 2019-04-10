#!/usr/bin/env groovy
/*
 * ReportPathDirectorException class
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
package org.fidata.gradle.utils

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Exception thrown by {@link PathDirector} when it is unable to determine path for reports for some task
 */
class ReportPathDirectorException extends RuntimeException {
  private ReportPathDirectorException(Object task, Throwable throwable) {
    super(String.format('Unable to determine path for reports for task %s', task), throwable)
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
