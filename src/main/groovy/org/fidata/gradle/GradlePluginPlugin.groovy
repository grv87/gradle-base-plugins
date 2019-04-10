#!/usr/bin/env groovy
/*
 * org.fidata.plugin Gradle plugin
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

import static JvmBasePlugin.FUNCTIONAL_TEST_SOURCE_SET_NAME
import static JvmBasePlugin.FUNCTIONAL_TEST_TASK_NAME
import static JvmBasePlugin.JUNIT_GROUP
import static JvmBasePlugin.JUNIT_MODULE
import static JvmBasePlugin.SPOCK_GROUP
import static JvmBasePlugin.SPOCK_MODULE
import static ProjectPlugin.ARTIFACTORY_URL
import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static org.gradle.internal.FileUtils.toSafeFileName
import com.gradle.publish.PluginBundleExtension
import com.gradle.publish.PublishTask
import groovy.transform.CompileStatic
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.fidata.gradle.utils.PathDirector
import org.fidata.gradle.utils.PluginDependeesUtils
import org.fidata.gradle.utils.ReportPathDirectorException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugin.devel.tasks.ValidateTaskProperties
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.ysb33r.gradle.gradletest.TestSet

/**
 * Provides an environment for a Gradle plugin project
 */
@CompileStatic
final class GradlePluginPlugin extends AbstractProjectPlugin implements PropertyChangeListener {
  @Override
  protected void doApply() {
    project.pluginManager.apply JvmBasePlugin

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, GradlePluginPluginDependees.PLUGIN_DEPENDEES

    project.plugins.getPlugin(ProjectPlugin).defaultProjectGroup = "${ -> ProjectPlugin.DEFAULT_PROJECT_GROUP }.gradle"

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener this

    configureTesting()

    if (!isBuildSrc) {
      configureArtifacts()

      configureReleases()
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
      default:
        project.logger.warn('org.fidata.plugin: unexpected property change source: {}', e.source)
    }
  }

  static final PathDirector<ValidateTaskProperties> VALIDATE_TASK_PROPERTIES_REPORT_DIRECTOR = new PathDirector<ValidateTaskProperties>() {
    @Override
    Path determinePath(ValidateTaskProperties object)  {
      try {
        Paths.get(toSafeFileName(object.name))
      } catch (InvalidPathException e) {
        throw new ReportPathDirectorException(object, e)
      }
    }
  }

  private static final Pattern COMPAT_TEST_TASK_NAME_PATTERN = ~/^compatTest(.+)/

  static final PathDirector<TaskProvider<Test>> COMPAT_TEST_REPORT_DIRECTOR = new PathDirector<TaskProvider<Test>>() {
    @SuppressWarnings('CatchIndexOutOfBoundsException')
    @Override
    Path determinePath(TaskProvider<Test> object)  {
      try {
        Paths.get('compatTest', toSafeFileName(((List<String>)(object.name =~ COMPAT_TEST_TASK_NAME_PATTERN)[0])[1].uncapitalize()))
      } catch (InvalidPathException | IndexOutOfBoundsException | IllegalStateException e) {
        throw new ReportPathDirectorException(object, e)
      }
    }
  }

  private void configureTesting() {
    project.tasks.withType(ValidateTaskProperties).configureEach { ValidateTaskProperties validateTaskProperties ->
      validateTaskProperties.with {
        outputFile.set project.convention.getPlugin(ProjectConvention).getTxtReportFile(VALIDATE_TASK_PROPERTIES_REPORT_DIRECTOR, validateTaskProperties)
        failOnWarning = true
      }
    }

    SourceSetContainer sourceSets = project.convention.getPlugin(JavaPluginConvention).sourceSets

    project.afterEvaluate {
      NamedDomainObjectProvider<SourceSet> gradleTestSourceSetProvider = sourceSets.named(TestSet.baseName(/* WORKAROUND: org.ysb33r.gradle.gradletest.Names.DEFAULT_TASK has package scope <> */ 'gradleTest'))
      gradleTestSourceSetProvider.configure { SourceSet gradleTestSourceSet ->
        // Clean up default dependencies provided by gradleTest
        project.configurations[gradleTestSourceSet.compileConfigurationName].dependencies.removeAll { Dependency dependency ->
          ExternalModuleDependency.isInstance(dependency) && (
            dependency.group == JUNIT_GROUP && dependency.name == JUNIT_MODULE ||
            dependency.group == SPOCK_GROUP && dependency.name == SPOCK_MODULE ||
          dependency.group == 'org.hamcrest' && dependency.name == 'hamcrest-core'
          )
        }
        null
      }
      project.plugins.getPlugin(JvmBasePlugin).addJUnitDependency gradleTestSourceSetProvider
      project.plugins.getPlugin(JvmBasePlugin).addSpockDependency gradleTestSourceSetProvider
    }

    project.plugins.getPlugin(JvmBasePlugin).addSpockDependency(
      sourceSets.named('compatTest'),
      /*
       * TOTEST:
       * Looks like there is no built-in way to get collection of TaskProvider.
       * This is workaround, but it could trigger creation of tasks
       * <grv87 2018-08-23>
       */
      (Iterable<TaskProvider<Test>>)(project.tasks.withType(Test).matching { Test test -> test.name =~ COMPAT_TEST_TASK_NAME_PATTERN }.collect { Test test -> project.tasks.withType(Test).named(test.name) }), COMPAT_TEST_REPORT_DIRECTOR
    )

    project.afterEvaluate {
      project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets((project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets + [sourceSets.getByName(TestSet.baseName(/* WORKAROUND: org.ysb33r.gradle.gradletest.Names.DEFAULT_TASK has package scope <> */ 'gradleTest')), sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME)]).toArray(new SourceSet[0]))
      project.extensions.getByType(GradlePluginDevelopmentExtension).testSourceSets.each { SourceSet sourceSet ->
        project.plugins.getPlugin(JvmBasePlugin).configureIntegrationTestSourceSetClasspath sourceSet
      }
    }

    project.tasks.matching { Task task -> task.name.startsWith('compatTest') || task.name == 'gradleTest' }.configureEach { Task task ->
      task.shouldRunAfter project.tasks.named(FUNCTIONAL_TEST_TASK_NAME)
    }
  }

  private void configureArtifacts() {
    project.plugins.findPlugin(JvmBasePlugin)?.mainPublicationName?.set 'pluginMaven' /* Hardcoded in MavenPluginPublishPlugin */

    project.afterEvaluate {
      if (project.convention.getPlugin(ProjectConvention).publicReleases) {
        project.plugins.findPlugin(JvmBasePlugin)?.with {
          sourcesJar.set project.tasks.withType(Jar).named(/*PublishPlugin.SOURCES_JAR_TASK_NAME*/ 'publishPluginJar')
          javadocJar.set project.tasks.withType(Jar).named(/*PublishPlugin.JAVA_DOCS_TASK_NAME*/ 'publishPluginJavaDocsJar')
        }
        project.plugins.findPlugin(GroovyBasePlugin)?.groovydocJar?.set project.tasks.withType(Jar).named(/*PublishPlugin.GROOVY_DOCS_TASK_NAME*/ 'publishPluginGroovyDocsJar')
      }
    }
  }

  private void configurePublicReleases() {
    RootProjectConvention rootProjectConvention = project.rootProject.convention.getPlugin(RootProjectConvention)
    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    if (projectConvention.publicReleases) {
      project.pluginManager.apply 'com.gradle.plugin-publish'
      project.extensions.extraProperties[PublishTask.GRADLE_PUBLISH_KEY] = project.extensions.extraProperties['gradlePluginsKey']
      project.extensions.extraProperties[PublishTask.GRADLE_PUBLISH_SECRET] = project.extensions.extraProperties['gradlePluginsSecret']
      project.extensions.configure(PluginBundleExtension) { PluginBundleExtension extension ->
        /*
         * WORKAROUND:
         * Groovy 2.5 don't let us use `with` here. Got error:
         * java.lang.ClassCastException: com.gradle.publish.PluginBundleExtension cannot be cast to groovy.lang.GroovyObject
         * Must be a bug in Groovy
         * <grv87 2018-12-02>
         */
        extension.description = project.version.toString() == '1.0.0' ? project.description : rootProjectConvention.changeLogTxt.get().toString()
        extension.tags = (Collection<String>)projectConvention.tags.get()
        extension.website = projectConvention.websiteUrl.get()
        extension.vcsUrl = rootProjectConvention.vcsUrl.get()
      }
      project.tasks.named(/* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins').configure { Task publishPlugins ->
        publishPlugins.onlyIf { rootProjectConvention.isRelease.get() }
      }
      project.rootProject.tasks.named(RELEASE_TASK_NAME).configure { Task release ->
        release.finalizedBy /* WORKAROUND: PublishPlugin.BASE_TASK_NAME has private scope <grv87 2018-06-23> */ 'publishPlugins'
      }
    }
  }

  private void configureReleases() {
    GString repository = "plugins-${ project.rootProject.convention.getPlugin(RootProjectConvention).isRelease.get() ? 'release' : 'snapshot' }"
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
        credentials.username = project.rootProject.extensions.extraProperties['artifactoryUser'].toString()
        credentials.password = project.rootProject.extensions.extraProperties['artifactoryPassword'].toString()
      }
    }

    configurePublicReleases()
  }
}
