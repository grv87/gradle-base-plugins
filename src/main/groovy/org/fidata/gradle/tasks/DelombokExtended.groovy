/*
 * DelombokExtended Gradle task class
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
package org.fidata.gradle.tasks

import groovy.transform.CompileStatic
import io.franzbecker.gradle.lombok.task.DelombokTask
import org.fidata.gradle.utils.InputDirectoryWrapper
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet

/**
 * Wrapper task for {@link DelombokTask}.
 * Overcomes difficulties with lazy list of input directories.
 * See discussion at <a hreh="https://github.com/franzbecker/gradle-lombok/pull/46">franzbecker/gradle-lombok#46</a>
 */
@CacheableTask
@CompileStatic
class DelombokExtended extends DelombokTask {
  @Override
  @Input
  String getMainClass() {
    super.mainClass
  }

  @Override
  @Internal
  // We don't actually use this and get compile classpath from sourceSet instead
  String getCompileConfigurationName() {
    super.compileConfigurationName
  }

  /**
   * Encoding of source files
   */
  @Optional
  @Input
  final Property<String> encoding = project.objects.property(String)

  /**
   * Source sets to pass through delombok.
   * The task gets from these source sets list of Java source files and also compile classpath
   */
  @Internal
  @SkipWhenEmpty
  final ListProperty<? extends SourceSet> sourceSets = project.objects.listProperty(SourceSet).empty()

  /**
   * Adds specified source set to {@link #sourceSets}.
   * {@link Provider}, {@link SourceSet} and {@link String} arguments are supported.
   * @param sourceSet Source set to add
   * @return sourceSets after addition
   * @throws IllegalArgumentException When argument of unsupported type is passed
   */
  ListProperty<? extends SourceSet> sourceSet(Object sourceSet) {
    if (Provider.isInstance(sourceSet)) {
      /*
       * TODO:
       * We suppose it is Provider<? extends SourceSet>.
       * It it provides something else (e.g. String or Iterable), extra work is required.
       * We have to find a way to determine actual type
       */
      sourceSets.add((Provider)sourceSet)
    } else if (SourceSet.isInstance(sourceSet)) {
      sourceSets.add((SourceSet)sourceSet)
    }
    else if (String.isInstance(sourceSet)) {
      sourceSets.add project.convention.getPlugin(JavaPluginConvention).sourceSets.named((String)sourceSet)
    } else {
      throw new IllegalArgumentException(sprintf('Unsupported argument type: %s', sourceSet))
    }
    sourceSets
  }

  /**
   * Adds specified source sets to {@link #sourceSets}
   * {@link Provider}, {@link SourceSet}, {@link String} and {@link Iterable} of those types are supported.
   * @param sourceSets Source sets to add
   * @return sourceSets after addition
   * @throws IllegalArgumentException When argument of unsupported type is encountered
   */
  @SuppressWarnings('ConfusingMethodName')
  ListProperty<? extends SourceSet> sourceSets(Object... sourceSets) {
    sourceSets.each { Object sourceSet ->
      if (Iterable.isInstance(sourceSet)) {
        (Iterable)sourceSet.each { Object innerSourceSet ->
          this.sourceSet innerSourceSet
        }
      } else {
        this.sourceSet sourceSet
      }
    }
    this.sourceSets
  }

  @InputFiles
  @PathSensitive(PathSensitivity.NONE)
  final FileCollection sourceSetsClasspath = project.files {
    sourceSets.get().collect { Object sourceSet -> ((SourceSet)sourceSet).compileClasspath }
  }

  /*
   * WORKAROUND:
   * We use Nested annotation and InputDirectoryWrapper
   * to emulate InputDirectories annotation
   * which Gradle doesn't have
   */
  @Nested
  @SkipWhenEmpty
  final Provider<List<InputDirectoryWrapper>> inputDirectories = project.providers.provider {
    (List<InputDirectoryWrapper>)sourceSets.get().collectMany { Object sourceSet -> ((SourceSet)sourceSet).java.sourceDirectories.collect { File dir -> new InputDirectoryWrapper(dir) } }
  }

  /**
   * Output directory
   */
  @OutputDirectory
  final DirectoryProperty outputDir = newOutputDirectory()

  @Override
  void exec() {
    outputDir.asFile.get().deleteDir()
    String encoding = this.encoding.orNull
    if (encoding) {
      args '--encoding', encoding
    }
    classpath sourceSetsClasspath
    inputDirectories.get().each { InputDirectoryWrapper dir ->
      args dir.value, '--target', outputDir.get()
    }
    super.exec()
  }
}
