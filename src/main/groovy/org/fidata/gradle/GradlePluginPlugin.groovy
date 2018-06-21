#!/usr/bin/env groovy
/*
 * org.fidata.plugin Gradle plugin
 * Copyright © 2017  Basil Peace
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
package org.fidata.gradle

import static GradlePluginPluginDependencies.PLUGIN_DEPENDENCIES
import static GroovyProjectPlugin.SPOCK_DISABLED_CODENARC_RULES
import static GroovyProjectPlugin.SPOCK_REPORTS_DIR_NAME
import static org.gradle.internal.FileUtils.toSafeFileName
import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
// import org.gradle.api.plugins.PluginContainer
// import org.gradle.api.tasks.TaskContainer
import com.gradle.publish.PublishPlugin
import com.gradle.publish.PluginBundleExtension
import com.gradle.publish.MavenCoordinates
import org.gradle.api.tasks.testing.Test
// import org.gradle.api.Action
// import org.gradle.api.tasks.testing.TestTaskReports
import org.gradle.plugin.devel.tasks.ValidateTaskProperties
import org.gradle.api.tasks.javadoc.Groovydoc
import java.util.Map
import java.util.regex.Matcher
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
// import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

/**
 * Provides an environment for a Gradle plugin project
 */
@CompileStatic
final class GradlePluginPlugin extends AbstractPlugin implements PropertyChangeListener {
  @Override
  void apply(Project project) {
    super.apply(project);
      // PluginContainer plugins = project.getPlugins()

    project.plugins.with {
      apply ProjectPlugin

      PLUGIN_DEPENDENCIES.findAll() { Map.Entry<String, ? extends Map> depNotation -> depNotation.value.getOrDefault('enabled', true) }.keySet().each { String id ->
        apply id
      }
    }

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener(this)
    configurePublicReleases()

      // TaskContainer tasks = getTasks()

    project.tasks.withType(Test) { Test task ->
      task.with {
        Matcher compatTestMatcher = (name =~ /^compatTest(.*)?/)
        if (compatTestMatcher.matches()) {
          // Project project = task.project
          String reportDirName = "compatTest/${ toSafeFileName(compatTestMatcher.group(1).uncapitalize()) }" /* uncapitalize requires Groovy >= 2.4.8, i.e. Gradle >= 3.5 */
          // TestTaskReports reports = task.getReports()
          // reports.junitXml.destination = new File(project.convention.getPlugin(ProjectConvention).xmlReportsDir, reportDirName) // TODO: Cannot set read-only property: destination
          if (project.plugins.hasPlugin(GroovyProjectPlugin)) {
            reports.html.enabled = false
            // Map<String, File> systemProperties = new Map<String, File>()
            // systemProperties.put('com.athaydes.spockframework.report.outputDir', new File(project.convention.getPlugin(ProjectConvention.class).getHtmlReportsDir(), "${ GroovyProjectPlugin.SPOCK_REPORTS_DIR_NAME }/$reportDirName").absolutePath)
            systemProperty 'com.athaydes.spockframework.report.outputDir', new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, "${ SPOCK_REPORTS_DIR_NAME }/$reportDirName").absolutePath
          }
        }
      }
    }
    project.tasks.withType(ValidateTaskProperties) { ValidateTaskProperties task ->
      task.with {
        outputFile.set new File(project.convention.getPlugin(ProjectConvention).txtReportsDir, "${ toSafeFileName(name) }.txt")
        failOnWarning = true
      }
    }
    project.tasks.withType(Groovydoc) { Groovydoc task ->
      task.with {
        link "https://docs.gradle.org/${ project.gradle.gradleVersion }/javadoc/", 'org.gradle.'
      }
    }

    project.dependencies.with {
      add('api', gradleApi())
      add('testImplementation', gradleTestKit())
    }

    project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName('compatTest') { SourceSet sourceSet ->
      sourceSet.compileClasspath += project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName('main').output + project.configurations.getByName('testCompileClasspath')
      sourceSet.runtimeClasspath += sourceSet.output + sourceSet.compileClasspath + project.configurations.getByName('testRuntimeClasspath')
    }

    project.tasks.getByName('codenarcCompatTest').extensions.extraProperties['disabledRules'] = SPOCK_DISABLED_CODENARC_RULES

    configureArtifactory()
  }

  /**
   * Gets called when a property is changed
   */
  void propertyChange(PropertyChangeEvent e) {
    switch (e.source) {
      case project.convention.getPlugin(ProjectConvention):
        switch (e.propertyName) {
          case 'publicReleases':
            configurePublicReleases()
            break
        }
      break
    }
  }

  private void configurePublicReleases() {
    if (project.convention.getPlugin(ProjectConvention).publicReleases) {
      project.plugins.apply 'com.gradle.plugin-publish'
      project.extensions.getByType(PluginBundleExtension).with {
        website = project.convention.getPlugin(ProjectConvention).websiteUrl
        vcsUrl = project.convention.getPlugin(ProjectConvention).vcsUrl
        description = { project.convention.getPlugin(ProjectConvention).changeLog.toString() }
        mavenCoordinates { MavenCoordinates mavenCoordinates
          mavenCoordinates.groupId = project.group
        }
      }
      project.tasks.getByName(/*PublishPlugin.PUBLISH_TASK_NAME*/ 'publishPlugins').onlyIf { project.convention.getPlugin(ProjectConvention).isRelease }
      project.tasks.getByName('release').finalizedBy 'publishPlugins'
    }
  }

  /*
   * CAVEAT:
   * project.Conventions and extensions in JFrog Gradle plugins have package scopes,
   * so we can't use static compilation
   * <grv87 2018-02-18>
   */
  @CompileDynamic
  private void configureArtifactory() {
    if (project.hasProperty('artifactoryUser') && project.hasProperty('artifactoryPassword')) {
      project.artifactory {
        resolve {
          repository {
            repoKey = project.convention.getPlugin(ProjectConvention).isRelease ? 'plugins-release' : 'plugins-snapshot'
          }
        }
        publish {
          repository {
            repoKey = project.convention.getPlugin(ProjectConvention).isRelease ? 'plugins-release-local' : 'plugins-snapshot-local'
          }
        }
      }
    }
  }
}
