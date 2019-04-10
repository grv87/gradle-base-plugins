/*
 * InputsOutputs Gradle task class
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
package org.fidata.gradle.tasks

import static java.nio.charset.StandardCharsets.UTF_8
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Generates reports about all task file inputs and outputs
 */
@CompileStatic
class InputsOutputs extends DefaultTask {
  /**
   * InputsOutputs default output file name
   */
  public static final String DEFAULT_OUTPUT_FILE_NAME = 'inputsOutputs.txt'

  /**
   * Output file
   * By default it is <code>${ reporting.baseDir }/inputsOutputs.txt</code>
   */
  @OutputFile
  final RegularFileProperty outputFile = project.objects.fileProperty()

  /**
   * Generates a report
   */
  @TaskAction
  void generate() {
    outputFile.get().asFile.withPrintWriter(UTF_8.name()) { PrintWriter writer ->
      for (Task t in project.tasks) {
        if (t.inputs.hasInputs) {
          for (File f in t.inputs.files) {
            writer.printf('%s input:\t%s\n', t.path, f.path)
          }
        }
        if (t.outputs.hasOutput) {
          for (File f in t.outputs.files) {
            writer.printf('%s output:\t%s\n', t.path, f.path)
          }
        }
      }
    }
  }

  InputsOutputs() {
    outputFile.set(project.extensions.getByType(ReportingExtension).baseDirectory.file(DEFAULT_OUTPUT_FILE_NAME))
    outputs.upToDateWhen { false }
  }
}
