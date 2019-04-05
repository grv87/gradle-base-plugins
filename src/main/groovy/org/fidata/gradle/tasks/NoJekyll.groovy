/*
 * NoJekyll Gradle task class
 * Copyright © 2017-2018  Basil Peace
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
package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
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

  private final DirectoryProperty destinationDir = project.objects.directoryProperty()

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
