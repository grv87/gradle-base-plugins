/*
 * NoJekyll Gradle task class
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
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
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

  private final DirectoryProperty destinationDir = project.layout.directoryProperty()

  /**
   * @return a dir where to generate a file
   */
  @Internal
  final DirectoryProperty getDestinationDir() {
    destinationDir
  }

  /**
   * @return a file to be generated
   */
  @OutputFile
  Provider<RegularFile> getDestinationFile() {
    destinationDir.file(FILE_NAME)
  }

  /**
   * Generates a file
   */
  @TaskAction
  void generate() {
    didWork = destinationFile.get().asFile.createNewFile()
  }
}
