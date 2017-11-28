#!/usr/bin/env groovy
/*
 * InputsOutputs task class
 * Copyright © 2015-2017  Basil Peace
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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.validation.constraints.NotNull
import org.gradle.api.Task

/**
 * Generates reports about all task file inputs and outputs
 */
class InputsOutputs extends DefaultTask {
  /**
   * InputsOutputs default output file name
   */
  public static final String DEFAULT_OUTPUT_FILE_NAME = 'inputsOutputs.txt'

  @NotNull
  private File outputFile

  /**
   * Gets output file
   */
  @OutputFile
  @NotNull
  File getOutputFile() {
    outputFile ?: project.reporting.file(DEFAULT_OUTPUT_FILE_NAME)
  }

  /**
   * Sets output file
   * By default it is '${reporting.baseDir}/inputsOutputs.txt`
   */
  void setOutputFile(@NotNull File outputFile) {
    this.outputFile = outputFile
  }

  /**
   * Generates a report
   */
  @TaskAction
  void generate() {
    outputFile.withPrintWriter('UTF-8') { PrintWriter writer ->
      for (Task t in project.tasks) {
        if (t.inputs.hasInputs) {
          for (File f in t.inputs.sourceFiles) {
            writer.println sprintf('%s input:\t%s', [t.path, f.path])
          }
        }
        if (t.outputs.hasOutput) {
          for (File f in t.outputs.files) {
            writer.println sprintf('%s output:\t%s', [t.path, f.path])
          }
        }
      }
    }
  }

  InputsOutputs() {
    outputs.upToDateWhen { false }
  }
}
