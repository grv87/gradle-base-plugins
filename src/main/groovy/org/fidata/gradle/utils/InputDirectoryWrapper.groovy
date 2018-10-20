/*
 * InputDirectoryWrapper class
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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * This class wraps single {@link File} instance
 * and marks it as {@link InputDirectory} for Gradle.
 * It is a workaround
 * for missing InputDirectories annotation
 */
@CompileStatic
class InputDirectoryWrapper {
  private final File value

  /**
   * Gets actual {@link File} value
   * @return Actual value
   */
  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  File getValue() {
    value
  }

  /**
   * Creates new InputDirectoryWrapper instance
   * @param value Actual {@link File} instance
   */
  InputDirectoryWrapper(File value) {
    this.value = value
  }
}
