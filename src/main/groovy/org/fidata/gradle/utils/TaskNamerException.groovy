#!/usr/bin/env groovy
/*
 * TaskNamerException class
 * Copyright © 2018  Basil Peace
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

/**
 * Exception thrown by {@link org.gradle.api.Namer} when it is unable to determine task name based on some other object
 *
 * @deprecated This exception is not used.
 * It will be removed in future versions.
 */
@Deprecated
class TaskNamerException extends RuntimeException {
  /**
   * Default constuctor
   * @param taskName General name of task for which path should have been determined
   * @param sourceType Type of source object
   * @param object Source object
   * @param throwable Original exception
   */
  TaskNamerException(String taskName, String sourceType, Object object, Throwable throwable) {
    super(String.format('Unable to determine %s task name for %s %s', taskName, sourceType, object), throwable)
  }
}
