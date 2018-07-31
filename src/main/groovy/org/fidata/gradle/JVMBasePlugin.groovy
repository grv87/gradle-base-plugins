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

import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static ProjectPlugin.LICENSE_FILE_NAMES
import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME
import org.gradle.api.Task
import groovy.transform.PackageScope
import org.gradle.plugins.signing.Sign
import org.gradle.api.file.CopySpec
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import org.fidata.gradle.utils.PluginDependeesUtils
import org.gradle.api.publish.PublishingExtension
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.fidata.gradle.tasks.CodeNarcTaskConvention
import org.gradle.api.artifacts.ModuleDependency
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.reporting.ReportingExtension
import org.ajoberstar.gradle.git.publish.GitPublishExtension
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayPublishTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import com.athaydes.spockframework.report.internal.ReportDataAggregator
import java.nio.file.Path
import java.nio.file.Paths
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.publish.PublicationContainer

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JVMBasePlugin extends AbstractPlugin implements PropertyChangeListener {
  /**
   * Name of jvm extension for {@link Project}
   */
  public static final String JVM_EXTENSION_NAME = 'jvm'

  @Override
  void apply(Project project) {
    super.apply(project)

    project.pluginManager.apply ProjectPlugin
    PluginDependeesUtils.applyPlugins project, JVMBasePluginDependees.PLUGIN_DEPENDEES

    project.extensions.add JVM_EXTENSION_NAME, new JVMBaseExtension(project)

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener this
    configurePublicReleases()

    project.tasks.withType(ProcessResources).configureEach { ProcessResources task ->
      task.from(LICENSE_FILE_NAMES) { CopySpec copySpec ->
        copySpec.into 'META-INF'
      }
    }

    configureTesting()

    configureDocumentation()

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
        project.logger.warn('org.fidata.base.jvm: unexpected property change source: {}', e.source.toString())
    }
  }

  private void configurePublicReleases() {
    if (project.convention.getPlugin(ProjectConvention).publicReleases) {
      configureBintray()
    }
  }

  /**
   * Adds JUnit dependency to specified source set configuration
   * @param sourceSet source set
   */
  void addJUnitDependency(SourceSet sourceSet) {
    project.dependencies.add(sourceSet.implementationConfigurationName, [
      group: 'junit',
      name: 'junit',
      version: '[4.0,5.0)'
    ])
  }

  /**
   * Adds Spock to specified source set and task
   * @param sourceSet source set
   * @param task test task.
   *        If null, task with the same name as source set is used
   */
  void addSpockDependency(SourceSet sourceSet, TaskProvider<Test> task = null) {
    addSpockDependency sourceSet, [task ?: project.tasks.withType(Test).named(sourceSet.name)], { String taskName -> Paths.get(taskName) }
  }

  /**
   * Adds Spock to specified source set and tasks
   * @param sourceSet source set
   * @param tasks list of test tasks.
   * @param reportDirNamer closure to set report directory name from task name
   *        Nested directories are supported.
   *        Call <code>org.gradle.internal.FileUtils.toSafeFileName</code> manually on individual directory/file names
   */
  void addSpockDependency(SourceSet sourceSet, Iterable<TaskProvider<Test>> tasks, Closure<Path> reportDirNamer) {
    addJUnitDependency sourceSet

    project.pluginManager.apply GroovyBasePlugin

    project.dependencies.with {
      add(sourceSet.implementationConfigurationName, [
        group  : 'org.spockframework',
        name   : 'spock-core',
        version: "1.1-groovy-${ ((List<String>)(GroovySystem.version =~ /^(\d+\.\d+)/)[0])[1] }"
      ]) { ModuleDependency dependency ->
        dependency.exclude(
          group: 'org.codehaus.groovy',
          module: 'groovy-all'
        )
      }
      add(sourceSet.runtimeOnlyConfigurationName, [
        group  : 'com.athaydes',
        name   : 'spock-reports',
        version: 'latest.release'
      ]) { ModuleDependency dependency ->
        dependency.transitive = false
      }
      add(sourceSet.implementationConfigurationName, [
        group  : 'org.slf4j',
        name   : 'slf4j-api',
        version: 'latest.release'
      ])
      add(sourceSet.implementationConfigurationName, [
        group  : 'org.slf4j',
        name   : 'slf4j-simple',
        version: 'latest.release'
      ])
    }
    project.plugins.withType(GroovyBasePlugin).configureEach { GroovyBasePlugin plugin ->
      plugin.addGroovyDependency project.configurations.getByName(sourceSet.implementationConfigurationName)
    }

    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    tasks.each { TaskProvider<Test> taskProvider ->
      taskProvider.configure { Test test ->
        GString reportDirName = "spock/${ reportDirNamer.call(test.name) }"
        File spockReportsDir = new File(projectConvention.htmlReportsDir, reportDirName)
        TaskProvider<Task> moveAggregatedReportProvider = project.tasks.register("move${ taskProvider.name.capitalize() }AggegatedReport") { Task moveAggregatedReport ->
          moveAggregatedReport.with {
            File aggregatedReportFile = new File(spockReportsDir, ReportDataAggregator.AGGREGATED_DATA_FILE.toString())
            destroyables.register aggregatedReportFile
            /*
             * WORKAROUND:
             * There is no built-in way to skip task if its single file input doesn't exist
             * https://github.com/gradle/gradle/issues/2919
             * <grv87 2018-07-25>
             */
            onlyIf { aggregatedReportFile.exists() }
            doLast {
              project.ant.invokeMethod('move', [
                file: aggregatedReportFile,
                todir: new File(projectConvention.jsonReportsDir, reportDirName)
              ])
            }
          }
        }
        test.with {
          reports.html.enabled = false
          systemProperty 'com.athaydes.spockframework.report.outputDir', spockReportsDir.absolutePath
          /*
           * WORKAROUND:
           * Spock Reports generates aggregated_report.json in the same directory as HTML files
           * https://github.com/renatoathaydes/spock-reports/issues/155
           * <grv87 2018-07-25>
           */
          finalizedBy moveAggregatedReportProvider
        }
        /*
         * WORKAROUND:
         * Without that we get error:
         * [Static type checking] - Cannot call org.gradle.api.tasks.TaskProvider <Test>#configure(org.gradle.api.Action
         * <java.lang.Object extends java.lang.Object>) with arguments [groovy.lang.Closure <org.gradle.api.Task>]
         * <grv87 2018-07-31>
         */
        null
      }
    }

    project.plugins.withType(GroovyBasePlugin).configureEach { GroovyBasePlugin plugin -> // TODO: 4.9
      project.tasks.withType(CodeNarc).named("codenarc${ sourceSet.name.capitalize() }").configure { Task task ->
        task.convention.getPlugin(CodeNarcTaskConvention).disabledRules.addAll 'MethodName', 'FactoryMethodName', 'JUnitPublicProperty', 'JUnitPublicNonTestMethod', /* WORKAROUND: https://github.com/CodeNarc/CodeNarc/issues/308 <grv87 2018-06-26> */ 'Indentation'
      }
    }
  }

  /**
   * Configures integration test source set classpath
   * See <a href="https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests">https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests</a>
   * @param sourceSet source set to configure
   */
  void configureIntegrationTestSourceSetClasspath(SourceSet sourceSet) {
    // https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests
    // +
    SourceSet mainSourceSet = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(MAIN_SOURCE_SET_NAME)
    sourceSet.compileClasspath += mainSourceSet.output
    sourceSet.runtimeClasspath += sourceSet.output + mainSourceSet.output // TOTEST

    project.configurations.getByName(sourceSet.implementationConfigurationName).extendsFrom project.configurations.getByName(mainSourceSet.implementationConfigurationName)
    project.configurations.getByName(sourceSet.runtimeOnlyConfigurationName).extendsFrom project.configurations.getByName(mainSourceSet.runtimeOnlyConfigurationName)
  }

  /**
   * Name of functional test source set
   */
  public static final String FUNCTIONAL_TEST_SOURCE_SET_NAME = 'functionalTest'
  /**
   * Name of functional test source directory
   */
  public static final String FUNCTIONAL_TEST_SRC_DIR_NAME = 'functionalTest'
  /**
   * Name of functional test task
   */
  public static final String FUNCTIONAL_TEST_TASK_NAME = 'functionalTest'
  /**
   * Name of functional test reports directory
   */
  public static final String FUNCTIONAL_TEST_REPORTS_DIR_NAME = 'functionalTest'

  /*
   * WORKAROUND:
   * Groovy error. Usage of `destination =` instead of setDestination leads to error:
   * [Static type checking] - Cannot set read-only property: destination
   * Also may be CodeNarc error
   * <grv87 2018-06-26>
   */
  @SuppressWarnings(['UnnecessarySetter'])
  private void configureFunctionalTests() {
    SourceSet functionalTestSourceSet = project.convention.getPlugin(JavaPluginConvention).sourceSets.create(FUNCTIONAL_TEST_SOURCE_SET_NAME) { SourceSet sourceSet ->
      sourceSet.java.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/java")
      sourceSet.resources.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/resources")

      configureIntegrationTestSourceSetClasspath sourceSet
    }

    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    TaskProvider<Test> functionalTestProvider = project.tasks.register(FUNCTIONAL_TEST_TASK_NAME, Test) { Test task ->
      task.with {
        group = 'Verification'
        description = 'Runs functional tests'
        shouldRunAfter project.tasks.named(TEST_TASK_NAME)
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
        reports.junitXml.setDestination new File(projectConvention.xmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
        reports.html.setDestination new File(projectConvention.htmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
      }
    }

    addSpockDependency functionalTestSourceSet, functionalTestProvider
  }

  private void configureTesting() {
    project.convention.getPlugin(JavaPluginConvention).testReportDirName = project.extensions.getByType(ReportingExtension).baseDir.toPath().relativize(new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, 'tests').toPath()).toString() // TODO: ???
    project.tasks.withType(Test).configureEach { Test task ->
      task.testLogging.exceptionFormat = TestExceptionFormat.FULL
    }

    addJUnitDependency project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(TEST_SOURCE_SET_NAME)

    configureFunctionalTests()
  }

  private void configureArtifactory() {
    project.convention.getPlugin(ArtifactoryPluginConvention).with {
      clientConfig.publisher.repoKey = "libs-${ project.convention.getPlugin(ProjectConvention).isRelease ? 'release' : 'snapshot' }-local"
      clientConfig.publisher.username = project.extensions.extraProperties['artifactoryUser']
      clientConfig.publisher.password = project.extensions.extraProperties['artifactoryPassword']
      clientConfig.publisher.maven = true
    }
    project.tasks.withType(ArtifactoryTask).named(ARTIFACTORY_PUBLISH_TASK_NAME).configure { ArtifactoryTask task ->
      PublicationContainer publications = project.extensions.getByType(PublishingExtension).publications
      publications.withType(MavenPublication) { MavenPublication mavenPublication ->
        task.mavenPublications.add mavenPublication
      }
      publications.whenObjectRemoved { MavenPublication mavenPublication ->
        task.mavenPublications.remove mavenPublication
      }

      task.dependsOn project.tasks.withType(Sign).matching { Sign sign -> // TODO
        publications.withType(MavenPublication).any { MavenPublication mavenPublication ->
          sign.name == "sign${ mavenPublication.name.capitalize() }Publication"
        }
      }
    }
    project.tasks.named(RELEASE_TASK_NAME).configure { Task task ->
      task.finalizedBy project.tasks.withType(ArtifactoryTask)
    }
  }

  /**
   * Name of maven java publication
   */
  public static final String MAVEN_JAVA_PUBICATION_NAME = 'mavenJava'

  @PackageScope
  boolean createMavenJavaPublication = true

  private void configureArtifactsPublishing() {
    project.afterEvaluate {
      if (createMavenJavaPublication) {
        project.extensions.getByType(PublishingExtension).publications.create(MAVEN_JAVA_PUBICATION_NAME, MavenPublication) { MavenPublication publication ->
          publication.from project.components.getByName('java' /* TODO */)
        }
      }
    }
    project.extensions.getByType(SigningExtension).sign project.extensions.getByType(PublishingExtension).publications

    configureArtifactory()
  }

  @SuppressWarnings(['UnnecessaryObjectReferences'])
  private void configureBintray() {
    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    project.pluginManager.apply 'com.jfrog.bintray'

    project.extensions.configure(BintrayExtension) { BintrayExtension extension ->
      extension.with {
        user = project.extensions.extraProperties['bintrayUser'].toString()
        key = project.extensions.extraProperties['bintrayAPIKey'].toString()
        pkg.repo = 'generic'
        pkg.name = 'gradle-project'
        pkg.userOrg = 'fidata'
        pkg.licenses = [projectConvention.license].toArray(new String[0])
        pkg.vcsUrl = projectConvention.vcsUrl
        pkg.desc = projectConvention.changeLog.toString()
        pkg.version.name = ''
        pkg.version.vcsTag = '' // TODO
        pkg.version.gpg.sign = true // TODO ?
        // pkg.version.attributes // Attributes to be attached to the version
      }
    }
    project.tasks.withType(BintrayPublishTask).configureEach { BintrayPublishTask task ->
      task.onlyIf { projectConvention.isRelease }
    }
    project.tasks.named(RELEASE_TASK_NAME).configure { Task task ->
      task.finalizedBy project.tasks.withType(BintrayPublishTask)
    }
  }

  private void configureDocumentation() {
    project.extensions.getByType(GitPublishExtension).contents.from(project.tasks.named(JAVADOC_TASK_NAME)).into "$project.version/javadoc"
  }
}
