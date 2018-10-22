#!/usr/bin/env groovy
/*
 * org.fidata.project.jdk Gradle plugin
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
package org.fidata.gradle

import static java.nio.charset.StandardCharsets.UTF_8
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.plugins.quality.Checkstyle
import io.franzbecker.gradle.lombok.LombokPluginExtension
import org.gradle.api.tasks.SourceSet
import groovy.transform.CompileStatic
import io.franzbecker.gradle.lombok.task.DelombokTask
import org.ajoberstar.gradle.git.publish.GitPublishExtension
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.fidata.gradle.utils.PluginDependeesUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.TaskProvider

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JavaProjectPlugin extends AbstractProjectPlugin {
  @Override
  void apply(Project project) {
    super.apply(project)

    project.pluginManager.apply JVMBasePlugin

    boolean isBuildSrc = project.project.convention.getPlugin(ProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, JavaProjectPluginDependees.PLUGIN_DEPENDEES

    configureLombok()

    if (!isBuildSrc) {
      configureDocumentation()
    }

    configureCodeQuality()
  }

  private void configureLombok() {
    project.extensions.configure(LombokPluginExtension) { LombokPluginExtension extension ->
      extension.with {
        version = 'latest.release'
        sha256 = ''
      }
    }
  }

  public static final String DELOMBOK_TASK_NAME = 'delombok'

  /*
   * WORKAROUND:
   * We have to use `setSource`, otherwise we got error:
   * Caused by: org.codehaus.groovy.runtime.typehandling.GroovyCastException: Cannot cast object
   * 'org.gradle.api.internal.tasks.DefaultTaskOutputs@198bfea7' with class
   * 'org.gradle.api.internal.tasks.DefaultTaskOutputs' to class 'org.gradle.api.file.FileTree'
   * <grv87 2018-08-01>
   */
  @SuppressWarnings('UnnecessarySetter')
  private void configureDelombok() {
    project.tasks.withType(Javadoc).named(JAVADOC_TASK_NAME).configure { Javadoc javadoc ->
      TaskProvider<DelombokTask> delombokProvider = project.tasks.register(DELOMBOK_TASK_NAME, DelombokTask) { DelombokTask delombok ->
        delombok.with {
          args '--encoding', UTF_8

          SourceSet mainSourceSet = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(MAIN_SOURCE_SET_NAME)

          dependsOn project.tasks.named(COMPILE_JAVA_TASK_NAME)
          File outputDir = new File(project.buildDir, 'delombok')
          outputs.dir outputDir
          mainSourceSet.java.srcDirs.each { File dir ->
            inputs.dir dir
            args dir, '--target', outputDir
          }
          classpath mainSourceSet.compileClasspath
          doFirst {
            outputDir.deleteDir()
          }
        }
      }
      javadoc.with {
        dependsOn delombokProvider
        setSource delombokProvider.get().outputs
      }
    }
  }

  private void configureDocumentation() {
    configureDelombok()

    project.extensions.getByType(GitPublishExtension).contents.from(project.tasks.named(JAVADOC_TASK_NAME)).into "$project.version/javadoc"
  }

  /**
   * Name of Checkstyle common task
   */
  public static final String CHECKSTYLE_TASK_NAME = 'checkstyle'

  private void configureCodeQuality() {
    project.plugins.getPlugin(ProjectPlugin).addCodeQualityCommonTask 'Checkstyle', CHECKSTYLE_TASK_NAME, Checkstyle
  }
}
