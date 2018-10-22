/*
 * InputsOutputs Gradle task class
 * Copyright © 2015-2018  Basil Peace
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

import static java.nio.charset.StandardCharsets.UTF_8
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task
import org.gradle.api.reporting.ReportingExtension

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
  final RegularFileProperty outputFile = newOutputFile()

  /**
   * Generates a report
   */
  @TaskAction
  void generate() {
    outputFile.get().asFile.withPrintWriter(UTF_8.name()) { PrintWriter writer ->
      for (Task t in project.tasks) {
        if (t.inputs.hasInputs) {
          for (File f in t.inputs.sourceFiles) {
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
