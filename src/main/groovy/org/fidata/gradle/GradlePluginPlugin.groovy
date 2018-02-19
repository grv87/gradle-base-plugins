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

    project.with {
      plugins.with {
        apply ProjectPlugin

        GradlePluginPluginDependencies.PLUGIN_DEPENDENCIES.findAll() { Map.Entry<String, ? extends Map> depNotation -> depNotation.value.getOrDefault('enabled', true) }.keySet().each { String id ->
          apply id
        }
      }

      convention.getPlugin(ProjectConvention).addPropertyChangeListener(this)
      configurePublicReleases()

      // TaskContainer tasks = getTasks()

      tasks.withType(Test) { Test task ->
        task.with {
          Matcher compatTestMatcher = (name =~ /^compatTest(.*)?/)
          if (compatTestMatcher.matches()) {
            // Project project = task.project
            String reportDirName = "compatTest/${ toSafeFileName(compatTestMatcher.group(1).uncapitalize()) }" /* uncapitalize requires Groovy >= 2.4.8, i.e. Gradle >= 3.5 */
            // TestTaskReports reports = task.getReports()
            // reports.junitXml.destination = new File(projectconvention.getPlugin(ProjectConvention).xmlReportsDir, reportDirName) // TODO: Cannot set read-only property: destination
            if (project.plugins.hasPlugin(GroovyProjectPlugin)) {
              reports.html.enabled = false
              // Map<String, File> systemProperties = new Map<String, File>()
              // systemProperties.put('com.athaydes.spockframework.report.outputDir', new File(project.getConvention().getPlugin(ProjectConvention.class).getHtmlReportsDir(), "${ GroovyProjectPlugin.SPOCK_REPORTS_DIR_NAME }/$reportDirName").absolutePath)
              systemProperty 'com.athaydes.spockframework.report.outputDir', new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, "${ GroovyProjectPlugin.SPOCK_REPORTS_DIR_NAME }/$reportDirName").absolutePath
            }
          }
        }
      }
      tasks.withType(ValidateTaskProperties) { ValidateTaskProperties task ->
        task.with {
          outputFile = new File(project.convention.getPlugin(ProjectConvention).txtReportsDir, "${ toSafeFileName(name) }.txt")
          failOnWarning = true
        }
      }
      tasks.withType(Groovydoc) { Groovydoc task ->
        task.with {
          link "https://docs.gradle.org/${ project.gradle.gradleVersion }/javadoc/", 'org.gradle.'
        }
      }

      dependencies.with {
        add('api', gradleApi())
        add('testImplementation', gradleTestKit())
      }

      convention.getByType(JavaPluginConvention).sourceSets.getByName('compatTest') { SourceSet sourceSet ->
        sourceSet.compileClasspath += convention.getByType(JavaPluginConvention).sourceSets.getByName('main').output + configurations.getByName('testCompileClasspath')
        sourceSet.runtimeClasspath += sourceSet.output + sourceSet.compileClasspath + configurations.getByName('testRuntimeClasspath')
      }

      tasks.getByName('codenarcCompatTest').setProperty 'disabledRules', GroovyProjectPlugin.SPOCK_DISABLED_CODENARC_RULES

      configureArtifactory()
    }
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
    project.with {
      if (convention.getByType(ProjectConvention).publicReleases) {
        plugins.apply 'com.gradle.plugin-publish'
        extensions.getByType(PluginBundleExtension).with {
          website = "https://github.com/FIDATA/$name"
          vcsUrl = "https://github.com/FIDATA/$name"
          description = convention.getByType(ProjectConvention).changeLog.toString()
          mavenCoordinates { MavenCoordinates mavenCoordinates
            mavenCoordinates.groupId = group
          }
        }
        tasks.getByName(/*PublishPlugin.PUBLISH_TASK_NAME*/ 'publishPlugins').onlyIf { convention.getByType(ProjectConvention).isRelease }
        tasks.getByName('release').finalizedBy 'publishPlugins'
      }
    }
  }

  /*
   * CAVEAT:
   * Conventions and extensions in JFrog Gradle plugins have package scopes,
   * so we can't use static compilation
   * <grv87 2018-02-18>
   */
  @CompileDynamic
  private void configureArtifactory() {
    if (project.hasProperty('artifactoryUser') && project.hasProperty('artifactoryPassword')) {
      project.with {
        artifactory {
          resolve {
            repository {
              repoKey = convention.getByType(ProjectConvention).isRelease ? 'plugins-release' : 'plugins-snapshot'
            }
          }
          publish {
            repository {
              repoKey = convention.getByType(ProjectConvention).isRelease ? 'plugins-release-local' : 'plugins-snapshot-local'
            }
          }
        }
      }
    }
  }
}