#!/usr/bin/env groovy
/*
 * TaskNamerException class
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

/**
 * Exception thrown by {@link org.gradle.api.Namer} when it is unable to determine task name based on some other object
 */
class TaskNamerException extends RuntimeException {
  /**
   * Default constuctor
   * @param taskName General name of task for which path should have been determined
   * @param sourceType Type of source object
   * @param object Source object
   * @param throwable Original exception
   */
  TaskNamerException(String taskName, String sourceType, Object object, Throwable throwable) {
    super(sprintf('Unable to determine %s task name for %s %s', taskName, sourceType, object), throwable)
  }
}
