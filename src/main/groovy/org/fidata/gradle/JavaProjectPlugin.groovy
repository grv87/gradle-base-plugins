#!/usr/bin/env groovy
/*
 * org.fidata.project.jdk Gradle plugin
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
package org.fidata.gradle

import static java.nio.charset.StandardCharsets.UTF_8
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import static org.gradle.internal.FileUtils.toSafeFileName
import com.google.common.io.Resources
import groovy.transform.CompileStatic
import io.franzbecker.gradle.lombok.LombokPluginExtension
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import org.ajoberstar.gradle.git.publish.GitPublishExtension
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.fidata.gradle.tasks.DelombokExtended
import org.fidata.gradle.utils.PathDirector
import org.fidata.gradle.utils.PluginDependeesUtils
import org.fidata.gradle.utils.ReportPathDirectorException
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JavaProjectPlugin extends AbstractProjectPlugin {
  @Override
  protected void doApply() {
    project.pluginManager.apply JvmBasePlugin

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, JavaProjectPluginDependees.PLUGIN_DEPENDEES

    configureLombok()

    if (!isBuildSrc) {
      configureDocumentation()
    }

    configureCodeQuality()

    if (!isBuildSrc) {
      configureArtifacts()
    }
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

  /**
   * Path director for codenarc reports
   */
  static final PathDirector<Checkstyle> CHECKSTYLE_REPORT_DIRECTOR = new PathDirector<Checkstyle>() {
    @Override
    Path determinePath(Checkstyle object)  {
      try {
        Paths.get(toSafeFileName((object.name - ~/^checkstyle/ /* WORKAROUND: CheckstylePlugin.getTaskBaseName has protected scope <grv87 2019-03-24> */).uncapitalize()))
      } catch (InvalidPathException e) {
        throw new ReportPathDirectorException(object, e)
      }
    }
  }

  /*
   * WORKAROUND:
   * Groovy error. Usage of `destination =` instead of setDestination leads to error:
   * [Static type checking] - Cannot set read-only property: destination
   * Also may be CodeNarc error
   * <grv87 2019-03-27>
   */
  @SuppressWarnings('UnnecessarySetter')
  private void configureCodeQuality() {
    project.plugins.getPlugin(ProjectPlugin).addCodeQualityCommonTask 'Checkstyle', CHECKSTYLE_TASK_NAME, Checkstyle

    project.extensions.configure(CheckstyleExtension) { CheckstyleExtension extension ->
      extension.toolVersion = '[8.19, 9['
      /*
       * WORKAROUND:
       * `fromUri` doesn't accept Charset.
       * See https://github.com/gradle/gradle/issues/8472 for discussion
       * <grv87 2019-03-24>
       */
      // extension.config = project.resources.text.fromUri(Resources.getResource(this.class, 'config/checkstyle/checkstyle.xml'))
      extension.config = project.resources.text.fromString(Resources.toString(Resources.getResource(this.class, 'config/checkstyle/checkstyle.xml'), UTF_8))
      extension.configProperties['basedir'] = project.rootDir.toString()
      extension.maxWarnings = 0
    }

    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    project.tasks.withType(Checkstyle).configureEach { Checkstyle checkstyle ->
      checkstyle.with {
        Path reportSubpath = Paths.get('checkstyle')
        reports.xml.enabled = true
        reports.xml.setDestination projectConvention.getXmlReportFile(reportSubpath, CHECKSTYLE_REPORT_DIRECTOR, checkstyle)
        reports.html.enabled = true
        reports.html.setDestination projectConvention.getHtmlReportFile(reportSubpath, CHECKSTYLE_REPORT_DIRECTOR, checkstyle)
      }
    }
  }

  private void configureArtifacts() {
    project.plugins.getPlugin(JvmBasePlugin).javadocJarProvider.configure { Jar javadocJar ->
      javadocJar.from project.tasks.withType(Javadoc).named(JAVADOC_TASK_NAME)
    }
  }
}
