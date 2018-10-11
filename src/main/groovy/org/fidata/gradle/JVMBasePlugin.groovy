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
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static ProjectPlugin.LICENSE_FILE_NAMES
import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME
import org.gradle.api.Namer
import org.fidata.gradle.utils.TaskNamerException
import org.fidata.gradle.utils.PathDirector
import org.fidata.gradle.utils.ReportPathDirectorException
import org.gradle.api.Task
import groovy.transform.PackageScope
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
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
import org.fidata.gradle.internal.AbstractProjectPlugin
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
import java.nio.file.Path
import java.nio.file.Paths
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.publish.PublicationContainer

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JVMBasePlugin extends AbstractProjectPlugin implements PropertyChangeListener {
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

    project.tasks.withType(JavaCompile).configureEach { JavaCompile javaCompile ->
      javaCompile.options.encoding = UTF_8.name()
    }

    project.tasks.withType(ProcessResources).configureEach { ProcessResources processResources ->
      processResources.from(LICENSE_FILE_NAMES) { CopySpec copySpec ->
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
        project.logger.warn('org.fidata.base.jvm: unexpected property change source: {}', e.source)
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
    addSpockDependency sourceSet, [task ?: project.tasks.withType(Test).named(sourceSet.name)], new PathDirector<TaskProvider<Test>>() {
      @Override
      @SuppressWarnings('CatchException')
      Path determinePath(TaskProvider<Test> object) throws ReportPathDirectorException {
        try {
          Paths.get(object.name)
        } catch (Exception e) {
          throw new ReportPathDirectorException(object, e)
        }
      }
    }
  }

  /**
   * Namer of codenarc task for source sets
   */
  static final Namer<SourceSet> CODENARC_NAMER = new Namer<SourceSet>() {
    @Override
    @SuppressWarnings('CatchException')
    String determineName(SourceSet sourceSet) throws TaskNamerException {
      try {
        "codenarc${ sourceSet.name.capitalize() }"
      } catch (Exception e) {
        throw new TaskNamerException('codenarc', 'source set', sourceSet, e)
      }
    }
  }

  /**
   * Adds Spock to specified source set and tasks
   * @param sourceSet source set
   * @param tasks list of test tasks.
   * @param reportDirector path director for task reports
   */
  void addSpockDependency(SourceSet sourceSet, Iterable<TaskProvider<Test>> tasks, PathDirector<TaskProvider<Test>> reportDirector) {
    addJUnitDependency sourceSet

    project.pluginManager.apply GroovyBasePlugin

    project.dependencies.with {
      add(sourceSet.implementationConfigurationName, [
        group  : 'org.spockframework',
        name   : 'spock-core',
        version: "1.2-groovy-${ (GroovySystem.version =~ /^\d+\.\d+/)[0] }"
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
    }
    project.plugins.withType(GroovyBasePlugin).configureEach { GroovyBasePlugin plugin ->
      plugin.addGroovyDependency project.configurations.getByName(sourceSet.implementationConfigurationName)
    }

    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    tasks.each { TaskProvider<Test> taskProvider ->
      taskProvider.configure { Test test ->
        test.with {
          reports.html.enabled = false
          File spockHtmlReportDir = projectConvention.getHtmlReportDir(reportDirector, taskProvider)
          File spockJsonReportDir = projectConvention.getJsonReportDir(reportDirector, taskProvider)
          systemProperty 'com.athaydes.spockframework.report.outputDir', spockHtmlReportDir.absolutePath
          systemProperty 'com.athaydes.spockframework.report.aggregatedJsonReportDir', spockJsonReportDir.absolutePath
          outputs.dir spockHtmlReportDir
          outputs.dir spockJsonReportDir
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
      project.tasks.withType(CodeNarc).named(CODENARC_NAMER.determineName(sourceSet)).configure { CodeNarc codenarc ->
        codenarc.convention.getPlugin(CodeNarcTaskConvention).disabledRules.addAll 'MethodName', 'FactoryMethodName', 'JUnitPublicProperty', 'JUnitPublicNonTestMethod'
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
  @Deprecated
  public static final String FUNCTIONAL_TEST_REPORTS_DIR_NAME = 'functionalTest'
  /**
   * Path of functional test reports directory
   */
  public static final Path FUNCTIONAL_TEST_REPORTS_PATH = Paths.get('functionalTest')

  /*
   * WORKAROUND:
   * Groovy error. Usage of `destination =` instead of setDestination leads to error:
   * [Static type checking] - Cannot set read-only property: destination
   * Also may be CodeNarc error
   * <grv87 2018-06-26>
   */
  @SuppressWarnings('UnnecessarySetter')
  private void configureFunctionalTests() {
    SourceSet functionalTestSourceSet = project.convention.getPlugin(JavaPluginConvention).sourceSets.create(FUNCTIONAL_TEST_SOURCE_SET_NAME) { SourceSet sourceSet ->
      sourceSet.java.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/java")
      sourceSet.resources.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/resources")

      configureIntegrationTestSourceSetClasspath sourceSet
    }

    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    TaskProvider<Test> functionalTestProvider = project.tasks.register(FUNCTIONAL_TEST_TASK_NAME, Test) { Test test ->
      test.with {
        group = 'Verification'
        description = 'Runs functional tests'
        shouldRunAfter project.tasks.named(TEST_TASK_NAME)
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
        reports.junitXml.setDestination projectConvention.getXmlReportDir(FUNCTIONAL_TEST_REPORTS_PATH)
        reports.html.setDestination projectConvention.getHtmlReportDir(FUNCTIONAL_TEST_REPORTS_PATH)
      }
    }

    addSpockDependency functionalTestSourceSet, functionalTestProvider
  }

  private void configureTesting() {
    project.convention.getPlugin(JavaPluginConvention).testReportDirName = project.extensions.getByType(ReportingExtension).baseDir.toPath().relativize(project.convention.getPlugin(ProjectConvention).htmlReportsDir.toPath()).toString()
    project.convention.getPlugin(JavaPluginConvention).testResultsDirName = project.buildDir.toPath().relativize(project.convention.getPlugin(ProjectConvention).xmlReportsDir.toPath()).toString()
    project.tasks.withType(Test).configureEach { Test test ->
      test.with {
        maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
        testLogging.exceptionFormat = TestExceptionFormat.FULL
      }
    }

    addJUnitDependency project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(TEST_SOURCE_SET_NAME)

    configureFunctionalTests()
  }

  /**
   * Namer of sign task for maven publications
   */
  static final Namer<MavenPublication> SIGN_MAVEN_PUBLICATION_NAMER = new Namer<MavenPublication>() {
    @Override
    @SuppressWarnings('CatchException')
    String determineName(MavenPublication mavenPublication) throws TaskNamerException {
      try {
        "sign${ mavenPublication.name.capitalize() }Publication"
      } catch (Exception e) {
        throw new TaskNamerException('sign', 'maven publication', mavenPublication, e)
      }
    }
  }

  private void configureArtifactory() {
    project.convention.getPlugin(ArtifactoryPluginConvention).with {
      clientConfig.publisher.repoKey = "libs-${ project.convention.getPlugin(ProjectConvention).isRelease.get() ? 'release' : 'snapshot' }-local"
      clientConfig.publisher.username = project.extensions.extraProperties['artifactoryUser'].toString()
      clientConfig.publisher.password = project.extensions.extraProperties['artifactoryPassword'].toString()
      clientConfig.publisher.maven = true
    }
    project.tasks.withType(ArtifactoryTask).named(ARTIFACTORY_PUBLISH_TASK_NAME).configure { ArtifactoryTask artifactoryPublish ->
      PublicationContainer publications = project.extensions.getByType(PublishingExtension).publications
      publications.withType(MavenPublication) { MavenPublication mavenPublication ->
        artifactoryPublish.mavenPublications.add mavenPublication
      }
      publications.whenObjectRemoved { MavenPublication mavenPublication ->
        artifactoryPublish.mavenPublications.remove mavenPublication
      }

      artifactoryPublish.dependsOn project.tasks.withType(Sign).matching { Sign sign -> // TODO
        publications.withType(MavenPublication).any { MavenPublication mavenPublication ->
          sign.name == SIGN_MAVEN_PUBLICATION_NAMER.determineName(mavenPublication)
        }
      }
    }
    project.tasks.named(RELEASE_TASK_NAME).configure { Task release ->
      release.finalizedBy project.tasks.withType(ArtifactoryTask)
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

  @SuppressWarnings(['UnnecessaryObjectReferences', 'UnnecessarySetter'])
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
        pkg.version.name = ''
        pkg.version.vcsTag = '' // TODO
        pkg.version.gpg.sign = true // TODO ?
        pkg.desc = project.version.toString() == '1.0.0' ? project.description : projectConvention.changeLogTxt.get().toString()
        pkg.labels = projectConvention.tags.get().toArray(new String[0])
        pkg.setLicenses projectConvention.license
        pkg.vcsUrl = projectConvention.vcsUrl.get()
        // pkg.version.attributes // Attributes to be attached to the version
      }
    }
    project.tasks.withType(BintrayPublishTask).configureEach { BintrayPublishTask bintrayPublish ->
      bintrayPublish.onlyIf { projectConvention.isRelease.get() }
    }
    project.tasks.named(RELEASE_TASK_NAME).configure { Task release ->
      release.finalizedBy project.tasks.withType(BintrayPublishTask)
    }
  }

  private void configureDocumentation() {
    project.tasks.withType(Javadoc).configureEach { Javadoc javadoc ->
      javadoc.with {
        options.encoding = UTF_8.name()
        /*
         * WORKAROUND:
         * https://github.com/gradle/gradle/issues/6168
         * <grv87 2018-08-01>
         */
        doFirst {
          destinationDir.deleteDir()
        }
      }
    }

    project.extensions.getByType(GitPublishExtension).contents.from(project.tasks.named(JAVADOC_TASK_NAME)).into "$project.version/javadoc"
  }
}
