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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.internal.plugins.DslObject
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

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JVMBasePlugin extends AbstractPlugin implements PropertyChangeListener {
  @Override
  void apply(Project project) {
    super.apply(project)

    project.pluginManager.apply ProjectPlugin
    PluginDependeesUtils.applyPlugins project, JVMBasePluginDependees.PLUGIN_DEPENDEES

    project.extensions.add 'jvm', new JVMBaseExtension(project)

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener this
    configurePublicReleases()

    project.tasks.withType(ProcessResources) { ProcessResources task ->
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
  void addSpockDependency(SourceSet sourceSet, Test task = null) {
    addSpockDependency sourceSet, [task ?: project.tasks.withType(Test).getByName(sourceSet.name)], Closure.IDENTITY
  }

  /**
   * Adds Spock to specified source set and tasks
   * @param sourceSet source set
   * @param tasks list of test tasks.
   * @param reportDirNamer closure to set report directory name from task name
   *        Nested directories are supported.
   *        Call <code>org.gradle.internal.FileUtils.toSafeFileName</code> manually on individual directory/file names
   */
  void addSpockDependency(SourceSet sourceSet, Iterable<Test> tasks, Closure<GString> reportDirNamer) {
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
    project.plugins.withType(GroovyBasePlugin) { GroovyBasePlugin plugin ->
      plugin.addGroovyDependency project.configurations.getByName(sourceSet.implementationConfigurationName)
    }

    tasks.each { Test task ->
      Task moveAggregatedReportTask
      GString reportDirName
      File spockReportsDir
      task.with {
        reportDirName = "spock/${ reportDirNamer.call(name) }"
        spockReportsDir = new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, reportDirName)
        reports.html.enabled = false
        systemProperty 'com.athaydes.spockframework.report.outputDir', spockReportsDir.absolutePath
        /*
         * WORKAROUND:
         * Spock Reports generates aggregated_report.json in the same directory as HTML files
         * https://github.com/renatoathaydes/spock-reports/issues/155
         * <grv87 2018-07-25>
         */
        moveAggregatedReportTask = project.tasks.create("move${ task.name.capitalize() }AggegatedReport")
        finalizedBy moveAggregatedReportTask
      }
      moveAggregatedReportTask.with {
        File aggregatedReportFile = new File(spockReportsDir, ReportDataAggregator.AGGREGATED_DATA_FILE.toString())
        inputs.file aggregatedReportFile
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
            todir: new File(project.convention.getPlugin(ProjectConvention).jsonReportsDir, reportDirName)
          ])
        }
      }
    }

    project.plugins.withType(GroovyBasePlugin) { GroovyBasePlugin plugin ->
      new DslObject(project.tasks.getByName("codenarc${ sourceSet.name.capitalize() }")).convention.getPlugin(CodeNarcTaskConvention).disabledRules.addAll(['MethodName', 'FactoryMethodName', 'JUnitPublicProperty', 'JUnitPublicNonTestMethod', /* WORKAROUND: https://github.com/CodeNarc/CodeNarc/issues/308 <grv87 2018-06-26> */ 'Indentation'])
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
    SourceSet functionalTestSourceSet = project.convention.getPlugin(JavaPluginConvention).with {
      sourceSets.create(FUNCTIONAL_TEST_SOURCE_SET_NAME) { SourceSet sourceSet ->
        sourceSet.java.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/java")
        sourceSet.resources.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/resources")

        configureIntegrationTestSourceSetClasspath sourceSet
      }
    }

    Test functionalTestTask = project.tasks.create(FUNCTIONAL_TEST_TASK_NAME, Test) { Test task ->
      task.with {
        group = 'Verification'
        description = 'Runs functional tests'
        shouldRunAfter project.tasks.getByName(TEST_TASK_NAME)
        testClassesDirs = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME).output.classesDirs
        classpath = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME).runtimeClasspath
        reports.junitXml.setDestination new File(project.convention.getPlugin(ProjectConvention).xmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
        reports.html.setDestination new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
      }
    }

    addSpockDependency functionalTestSourceSet, functionalTestTask
  }

  private void configureTesting() {
    project.convention.getPlugin(JavaPluginConvention).testReportDirName = project.extensions.getByType(ReportingExtension).baseDir.toPath().relativize(new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, 'tests').toPath()).toString() // TODO: ???
    project.tasks.withType(Test) { Test task ->
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
    project.tasks.withType(ArtifactoryTask).getByName(ARTIFACTORY_PUBLISH_TASK_NAME).with { ArtifactoryTask task ->
      project.extensions.getByType(PublishingExtension).publications.withType(MavenPublication) { MavenPublication mavenPublication ->
        task.mavenPublications.add mavenPublication
      }
      project.extensions.getByType(PublishingExtension).publications.whenObjectRemoved { MavenPublication mavenPublication ->
        task.mavenPublications.remove mavenPublication
      }

      task.dependsOn project.tasks.withType(Sign).matching { Sign sign ->
        project.extensions.getByType(PublishingExtension).publications.withType(MavenPublication).any { MavenPublication mavenPublication ->
          sign.name == "sign${ mavenPublication.name.capitalize() }Publication"
        }
      }
    }
    project.tasks.getByName(RELEASE_TASK_NAME).finalizedBy project.tasks.withType(ArtifactoryTask)
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
    project.pluginManager.apply 'com.jfrog.bintray'

    project.extensions.getByType(BintrayExtension).with {
      user = project.extensions.extraProperties['bintrayUser'].toString()
      key = project.extensions.extraProperties['bintrayAPIKey'].toString()
      pkg.repo = 'generic'
      pkg.name = 'gradle-project'
      pkg.userOrg = 'fidata'
      pkg.licenses = [project.convention.getPlugin(ProjectConvention).license].toArray(new String[0])
      pkg.vcsUrl = project.convention.getPlugin(ProjectConvention).vcsUrl
      pkg.desc = project.convention.getPlugin(ProjectConvention).changeLog.toString()
      pkg.version.name = ''
      pkg.version.vcsTag = '' // TODO
      pkg.version.gpg.sign = true // TODO ?
      // pkg.version.attributes // Attributes to be attached to the version
    }
    project.tasks.getByName(RELEASE_TASK_NAME).finalizedBy project.tasks.withType(BintrayPublishTask)
  }

  private void configureDocumentation() {
    project.extensions.getByType(GitPublishExtension).contents.from(project.tasks.getByName(JAVADOC_TASK_NAME)).into "$project.version/javadoc"
  }
}
