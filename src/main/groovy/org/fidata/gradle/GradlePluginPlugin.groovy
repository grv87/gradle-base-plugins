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

import static ProjectPlugin.PREREQUISITES_UPDATE_TASK_NAME
import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static ProjectPlugin.ARTIFACTORY_URL
import static JVMBasePlugin.FUNCTIONAL_TEST_SOURCE_SET_NAME
import static org.gradle.internal.FileUtils.toSafeFileName
import org.gradle.api.tasks.SourceSet
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.fidata.gradle.utils.PluginDependeesUtils
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import java.util.regex.Pattern
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
import com.gradle.publish.PluginBundleExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.tasks.ValidateTaskProperties
import java.util.regex.Matcher
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import org.gradle.api.plugins.JavaPluginConvention
import org.ysb33r.gradle.gradletest.TestSet

/**
 * Provides an environment for a Gradle plugin project
 */
@CompileStatic
final class GradlePluginPlugin extends AbstractPlugin implements PropertyChangeListener {
  @Override
  void apply(Project project) {
    super.apply(project)

    project.pluginManager.apply JVMBasePlugin
    PluginDependeesUtils.applyPlugins project, GradlePluginPluginDependees.PLUGIN_DEPENDEES

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener(this)
    configurePublicReleases()

    configureBuildToolsLifecycle()

    configureDocumentation()

    configureTesting()

    configureArtifactsPublishing()
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
      project.pluginManager.apply 'com.gradle.plugin-publish'
      project.extensions.getByType(PluginBundleExtension).with {
        website = project.convention.getPlugin(ProjectConvention).websiteUrl
        vcsUrl = project.convention.getPlugin(ProjectConvention).vcsUrl
        description = project.convention.getPlugin(ProjectConvention).changeLog.toString()
      }
      project.tasks.getByName(/* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins').onlyIf { project.convention.getPlugin(ProjectConvention).isRelease }
      project.tasks.getByName(RELEASE_TASK_NAME).finalizedBy /* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins'
    }
  }

  private void configureBuildToolsLifecycle() {
    project.tasks.getByName(PREREQUISITES_UPDATE_TASK_NAME).dependsOn project.tasks.getByName('stutterWriteLocks')
  }

  private void configureTesting() {
    project.tasks.withType(ValidateTaskProperties) { ValidateTaskProperties task ->
      task.with {
        outputFile.set new File(project.convention.getPlugin(ProjectConvention).txtReportsDir, "${ toSafeFileName(name) }.txt")
        failOnWarning = true
      }
    }

    SourceSet gradleTestSourceSet = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(TestSet.baseName(/* WORKAROUND: org.ysb33r.gradle.gradletest.Names.DEFAULT_TASK has package scope <> */ 'gradleTest'))
    project.plugins.getPlugin(JVMBasePlugin).addSpockDependency gradleTestSourceSet

    Pattern compatTestPattern = ~/^compatTest(.+)/
    project.plugins.getPlugin(JVMBasePlugin).addSpockDependency(
      project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName('compatTest'),
      project.tasks.withType(Test).matching { Test task -> task.name =~ compatTestPattern }
    ) { String name ->
      Matcher compatTestMatcher = (name =~ compatTestPattern)
      compatTestMatcher.find()
      "compatTest/${ compatTestMatcher.group(1).uncapitalize() }"
    }

    project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets((project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets + [gradleTestSourceSet, project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME)]).toArray(new SourceSet[0]))
    project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets.each { SourceSet sourceSet ->
      project.plugins.getPlugin(JVMBasePlugin).configureIntegrationTestSourceSetClasspath sourceSet
    }
  }

  private void configureDocumentation() {
    project.extensions.getByType(JVMBaseExtension).javadocLinks['org.gradle'] = project.uri("https://docs.gradle.org/${ project.gradle.gradleVersion }/javadoc/")
  }

  private void configureArtifactsPublishing() {
    GString repository = "plugins-${ project.convention.getPlugin(ProjectConvention).isRelease ? 'release' : 'snapshot' }"
    project.convention.getPlugin(ArtifactoryPluginConvention).clientConfig.with {
      publisher.repoKey = "$repository-local"
    }
    project.repositories.maven { MavenArtifactRepository mavenArtifactRepository ->
      mavenArtifactRepository.with {
        /*
         * WORKAROUND:
         * Groovy bug?
         * When GString is used, URI property setter is called anyway, and we got cast error
         * <grv87 2018-06-26>
         */
        url = project.uri("$ARTIFACTORY_URL/$repository/")
        credentials.username = project.extensions.extraProperties['artifactoryUser']
        credentials.password = project.extensions.extraProperties['artifactoryPassword']
      }
    }
  }
}
