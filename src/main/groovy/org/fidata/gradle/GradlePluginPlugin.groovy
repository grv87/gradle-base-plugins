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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle

import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static ProjectPlugin.ARTIFACTORY_URL
import static JVMBasePlugin.FUNCTIONAL_TEST_SOURCE_SET_NAME
import static JVMBasePlugin.FUNCTIONAL_TEST_TASK_NAME
import static org.gradle.internal.FileUtils.toSafeFileName
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.fidata.gradle.utils.PluginDependeesUtils
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import java.util.regex.Pattern
import java.nio.file.Paths
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
import org.gradle.api.tasks.TaskProvider

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

    project.plugins.getPlugin(ProjectPlugin).defaultProjectGroup = 'org.fidata.gradle'

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener(this)
    configurePublicReleases()

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
      default:
        project.logger.warn('org.fidata.plugin: unexpected property change source: {}', e.source.toString())
    }
  }

  private void configurePublicReleases() {
    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    if (projectConvention.publicReleases) {
      project.pluginManager.apply 'com.gradle.plugin-publish'
      project.extensions.configure(PluginBundleExtension) { PluginBundleExtension extension ->
        extension.with {
          website = projectConvention.websiteUrl.get()
          vcsUrl = projectConvention.vcsUrl.get()
          description = project.version.toString() == '1.0.0' ? project.description : projectConvention.changeLogTxt.get().toString()
          tags = (Collection<String>)projectConvention.tags.get()
        }
      }
      project.tasks.named(/* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins').configure { Task task ->
        task.onlyIf { projectConvention.isRelease.get() }
      }
      project.tasks.named(RELEASE_TASK_NAME).configure { Task task ->
        task.finalizedBy /* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins'
      }
    }
  }

  private void configureTesting() {
    project.tasks.withType(ValidateTaskProperties).configureEach { ValidateTaskProperties task ->
      task.with {
        outputFile.set new File(project.convention.getPlugin(ProjectConvention).txtReportsDir, "${ toSafeFileName(name) }.txt")
        failOnWarning = true
      }
    }

    SourceSetContainer sourceSets = project.convention.getPlugin(JavaPluginConvention).sourceSets

    project.afterEvaluate {
      project.plugins.getPlugin(JVMBasePlugin).addSpockDependency sourceSets.getByName(TestSet.baseName(/* WORKAROUND: org.ysb33r.gradle.gradletest.Names.DEFAULT_TASK has package scope <> */ 'gradleTest'))
    }

    Pattern compatTestPattern = ~/^compatTest(.+)/
    project.plugins.getPlugin(JVMBasePlugin).addSpockDependency(
      sourceSets.getByName('compatTest'),
      /*
       * Looks like there is no built-in way to get collection of TaskProvider
       */
      (Iterable<TaskProvider<Test>>)(project.tasks.withType(Test).matching { Test task -> task.name =~ compatTestPattern }.collect { Test task -> project.tasks.withType(Test).named(task.name) })
    ) { String taskName ->
      Matcher compatTestMatcher = (taskName =~ compatTestPattern)
      compatTestMatcher.find()
      Paths.get('compatTest', toSafeFileName(compatTestMatcher.group(1).uncapitalize()))
    }

    project.afterEvaluate {
      project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets((project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets + [sourceSets.getByName(TestSet.baseName(/* WORKAROUND: org.ysb33r.gradle.gradletest.Names.DEFAULT_TASK has package scope <> */ 'gradleTest')), sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME)]).toArray(new SourceSet[0]))
      // https://docs.gradle.org/current/userguide/test_kit.html#sub:test-kit-automatic-classpath-injection
      project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets.each { SourceSet sourceSet ->
        /*SourceSet mainSourceSet = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(MAIN_SOURCE_SET_NAME)
        sourceSet.compileClasspath += mainSourceSet.output
        sourceSet.runtimeClasspath += sourceSet.output + mainSourceSet.output*/
        project.plugins.getPlugin(JVMBasePlugin).configureIntegrationTestSourceSetClasspath sourceSet
      }
    }

    project.tasks.matching { Task task -> task.name ==~ ~/^compatTest/ || task.name == 'gradleTest' }.configureEach { Task task ->
      task.shouldRunAfter project.tasks.named(FUNCTIONAL_TEST_TASK_NAME)
    }
  }

  private void configureDocumentation() {
    project.extensions.getByType(JVMBaseExtension).javadocLinks['org.gradle'] = project.uri("https://docs.gradle.org/${ project.gradle.gradleVersion }/javadoc/")
  }

  private void configureArtifactsPublishing() {
    project.plugins.getPlugin(JVMBasePlugin).createMavenJavaPublication = false

    GString repository = "plugins-${ project.convention.getPlugin(ProjectConvention).isRelease.get() ? 'release' : 'snapshot' }"
    project.convention.getPlugin(ArtifactoryPluginConvention).clientConfig.publisher.repoKey = "$repository-local"
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
