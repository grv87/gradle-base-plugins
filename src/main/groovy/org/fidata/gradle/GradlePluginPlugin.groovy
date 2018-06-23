#!/usr/bin/env groovy
/*
 * org.fidata.plugin Gradle plugin
 * Copyright © 2017-2018  Basil Peace
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

import com.gradle.publish.PublishPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention

import java.util.regex.Pattern

import static GradlePluginPluginDependencies.PLUGIN_DEPENDENCIES
import static ProjectPlugin.BUILD_TOOLS_UPDATE_TASK_NAME
import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static org.gradle.internal.FileUtils.toSafeFileName
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
import com.gradle.publish.PluginBundleExtension
import com.gradle.publish.MavenCoordinates
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.tasks.ValidateTaskProperties
import org.gradle.api.tasks.javadoc.Groovydoc
import java.util.regex.Matcher
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import org.gradle.api.plugins.JavaPluginConvention

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

    configureGroovydoc()

    configureBuildToolsLifecycle()

    configureCompatTest()

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
        description = project.convention.getPlugin(ProjectConvention).changeLog.toString()
        mavenCoordinates { MavenCoordinates mavenCoordinates ->
          mavenCoordinates.groupId = project.group.toString()
        }
      }
      project.tasks.getByName(/* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins').onlyIf { project.convention.getPlugin(ProjectConvention).isRelease }
      project.tasks.getByName(RELEASE_TASK_NAME).finalizedBy /* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins'
    }
  }

  private void configureBuildToolsLifecycle() {
    project.tasks.getByName(BUILD_TOOLS_UPDATE_TASK_NAME).dependsOn project.tasks.getByName('stutterWriteLocks')
  }

  private void configureCompatTest() {
    Pattern compatTestPattern = ~/^compatTest(.+)/
    project.plugins.getPlugin(JDKProjectPlugin).addSpockDependency(
      project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName('compatTest'),
      project.tasks.withType(Test).matching({ Test task -> task.name =~ compatTestPattern })
    ) { String name ->
      Matcher compatTestMatcher = (name =~ compatTestPattern)
      compatTestMatcher.find()
      "compatTest/${ compatTestMatcher.group(1).uncapitalize() }"
    }
    project.tasks.withType(ValidateTaskProperties) { ValidateTaskProperties task ->
      task.with {
        outputFile.set new File(project.convention.getPlugin(ProjectConvention).txtReportsDir, "${ toSafeFileName(name) }.txt")
        failOnWarning = true
      }
    }
    /* project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets.each */ // TODO
    project.plugins.getPlugin(JDKProjectPlugin).configureIntegrationTestSourceSetClasspath project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName('compatTest')
  }

  private void configureGroovydoc() {
    project.tasks.withType(Groovydoc) { Groovydoc task ->
      task.with {
        link "https://docs.gradle.org/${ project.gradle.gradleVersion }/javadoc/", 'org.gradle.'
      }
    }
  }

  private void configureArtifactory() {
    if (project.hasProperty('artifactoryUser') && project.hasProperty('artifactoryPassword')) {
      project.convention.getPlugin(ArtifactoryPluginConvention).clientConfig.with {
        resolver.repoKey = project.convention.getPlugin(ProjectConvention).isRelease ? 'plugins-release' : 'plugins-snapshot'
        publisher.repoKey = project.convention.getPlugin(ProjectConvention).isRelease ? 'plugins-release-local' : 'plugins-snapshot-local'
      }
    }
  }
}
