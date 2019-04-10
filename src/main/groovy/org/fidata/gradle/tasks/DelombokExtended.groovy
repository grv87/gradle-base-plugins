/*
 * DelombokExtended Gradle task class
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

import groovy.transform.CompileStatic
import groovy.transform.Internal
import io.franzbecker.gradle.lombok.task.DelombokTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal as GradleInternal
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
  @GradleInternal
  @Override
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
  @GradleInternal
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
    /*
     * TODO:
     * Maybe we should check for CharSequence instead of String,
     * to support GString too ?
     * <grv87 2019-03-24>
     */
    if (Provider.isInstance(sourceSet)) {
      sourceSets.add project.providers.provider { ->
        Object value = ((Provider)sourceSet).get()
        if (SourceSet.isInstance(value)) {
          return (SourceSet)value
        }
        else if (String.isInstance(sourceSet)) {
          return project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName((String) sourceSet)
        }
        throw new IllegalArgumentException(sprintf('Unsupported argument type: %s', sourceSet.class))
      }
    } else if (SourceSet.isInstance(sourceSet)) {
      sourceSets.add((SourceSet)sourceSet)
    }
    else if (String.isInstance(sourceSet)) {
      sourceSets.add project.convention.getPlugin(JavaPluginConvention).sourceSets.named((String)sourceSet)
    } else {
      throw new IllegalArgumentException(sprintf('Unsupported argument type: %s', sourceSet.class))
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

  @Classpath
  final FileCollection sourceSetsClasspath = project.files {
    sourceSets.get().collect { Object sourceSet -> ((SourceSet)sourceSet).compileClasspath }
  }

  @Internal
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @SkipWhenEmpty
  final Provider<Set<File>> inputDirectories = project.providers.provider {
    (Set<File>)sourceSets.get().collectMany { Object sourceSet -> ((SourceSet)sourceSet).java.srcDirs }
  }

  /**
   * Output directory
   */
  @OutputDirectory
  final DirectoryProperty outputDir = project.objects.directoryProperty()

  @Override
  void exec() {
    outputDir.asFile.get().deleteDir()
    String encoding = this.encoding.orNull
    if (encoding) {
      args '--encoding', encoding
    }
    /*
     * CAVEAT:
     * We still need to set classpath
     * even if @Classpath annotation was used
     * <grv87 2018-03-24>
     */
    classpath sourceSetsClasspath
    sourceSets.get().each { Object sourceSet ->
      ((SourceSet)sourceSet).java.srcDirs.each { File srcDir ->
        args  srcDir, '--target', outputDir.get()
      }
    }
    super.exec()
  }
}
