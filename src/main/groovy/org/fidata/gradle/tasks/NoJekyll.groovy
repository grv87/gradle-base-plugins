/*
 * NoJekyll task class
 * Copyright © 2017-2018  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Generates .nojekyll file in specified location
 */
@CacheableTask
@CompileStatic
class NoJekyll extends DefaultTask {
  /**
   * NoJekyll file name
   */
  public static final String FILE_NAME = '.nojekyll'

  private File destinationDir

  /**
   * @return a dir where to generate a file
   */
  @Internal
  File getDestinationDir() {
    destinationDir
  }

  /**
   * Sets a dir where to generate a file
   */
  void setDestinationDir(File newValue) {
    this.destinationDir = newValue
    this.destinationFile = new File(destinationDir, FILE_NAME)
  }

  private File destinationFile

  /**
   * @return a file that to be generated
   */
  @OutputFile
  File getDestinationFile() {
    destinationFile
  }

  /**
   * Generates a file
   */
  @TaskAction
  void generate() {
    destinationFile.text = ''
  }
}
