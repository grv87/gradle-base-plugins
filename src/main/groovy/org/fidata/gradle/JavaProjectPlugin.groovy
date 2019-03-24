#!/usr/bin/env groovy
/*
 * org.fidata.project.jdk Gradle plugin
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
package org.fidata.gradle

import static java.nio.charset.StandardCharsets.UTF_8
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.plugins.quality.Checkstyle
import io.franzbecker.gradle.lombok.LombokPluginExtension
import groovy.transform.CompileStatic
import org.fidata.gradle.tasks.DelombokExtended
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

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

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
        /*
         * CAVEAT:
         * Lombok should be 1.18.1+.
         * See https://github.com/rzwitserloot/lombok/issues/1782
         */
        version = '[1.18.1, 2['
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
    TaskProvider<DelombokExtended> delombokProvider = project.tasks.register(DELOMBOK_TASK_NAME, DelombokExtended) { DelombokExtended delombok ->
      delombok.with {
        encoding.set UTF_8.toString()
        sourceSet project.convention.getPlugin(JavaPluginConvention).sourceSets.named(MAIN_SOURCE_SET_NAME)

        dependsOn project.tasks.named(COMPILE_JAVA_TASK_NAME)

        outputDir.set new File(project.buildDir, 'delombok')
      }
      null
    }
    project.tasks.withType(Javadoc).named(JAVADOC_TASK_NAME).configure { Javadoc javadoc ->
      javadoc.with {
        dependsOn delombokProvider
        setSource delombokProvider.get().outputs
      }
    }
  }

  private void configureDocumentation() {
    configureDelombok()

    project.rootProject.extensions.getByType(GitPublishExtension).contents.from(project.tasks.named(JAVADOC_TASK_NAME)).into "$project.version/javadoc"
  }

  /**
   * Name of Checkstyle common task
   */
  public static final String CHECKSTYLE_TASK_NAME = 'checkstyle'

  private void configureCodeQuality() {
    project.plugins.getPlugin(ProjectPlugin).addCodeQualityCommonTask 'Checkstyle', CHECKSTYLE_TASK_NAME, Checkstyle
  }
}
