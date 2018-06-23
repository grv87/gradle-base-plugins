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

import groovy.transform.PackageScope
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

import static JDKProjectPluginDependencies.PLUGIN_DEPENDENCIES
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static ProjectPlugin.LICENSE_FILE_NAMES
import static ProjectPlugin.ARTIFACTORY_URL
import org.fidata.gradle.tasks.CodeNarcTaskConvention
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.internal.plugins.DslObject
import org.spdx.rdfparser.license.AnyLicenseInfo
import org.spdx.rdfparser.license.LicenseSet
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
import io.franzbecker.gradle.lombok.task.DelombokTask

import static org.gradle.internal.FileUtils.toSafeFileName

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JDKProjectPlugin extends AbstractPlugin implements PropertyChangeListener {
  @Override
  void apply(Project project) {
    super.apply(project)
    project.plugins.with {
      apply ProjectPlugin

      PLUGIN_DEPENDENCIES.findAll() { Map.Entry<String, ? extends Map> depNotation -> depNotation.value.getOrDefault('enabled', true) }.keySet().each { String id ->
        apply id
      }
    }

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener this
    configurePublicReleases()

    project.tasks.withType(ProcessResources) { ProcessResources task ->
      task.from(LICENSE_FILE_NAMES).into 'META-INF'
    }

    configureJavadoc()

    configureDelombok()

    addJUnitDependency project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(TEST_SOURCE_SET_NAME)

    configureFunctionalTests()

    project.convention.getPlugin(JavaPluginConvention).testReportDirName = project.extensions.getByType(ReportingExtension).baseDir.toPath().relativize(new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, 'tests').toPath()).toString() // TODO: ???

    // project.extensions.getByType(SigningExtension).sign project.extensions.getByType(PublishingExtension).publications
    // TODO Caused by: org.gradle.api.InvalidUserDataException: Cannot configure the 'publishing' extension after it has been accessed.

    configureArtifactory()

    project.extensions.getByType(GitPublishExtension).contents.from(project.tasks.getByName(JAVADOC_TASK_NAME)).into "$project.version/javadoc"
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

  public String DELOMBOK_TASK_NAME = 'delombok'

  private void configureDelombok() {
    DelombokTask delombokTask = project.tasks.create(DELOMBOK_TASK_NAME, DelombokTask) { DelombokTask task ->
      task.with {
        dependsOn project.tasks.getByName(COMPILE_JAVA_TASK_NAME)
        File outputDir = new File(project.buildDir, 'delombok')
        outputs.dir outputDir
        project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(MAIN_SOURCE_SET_NAME).java.srcDirs.each { File dir ->
          inputs.dir dir
          args dir, "-d", outputDir
        }
        classpath project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(MAIN_SOURCE_SET_NAME).compileClasspath
      }
    }
    project.tasks.withType(Javadoc).getByName(JAVADOC_TASK_NAME) { Javadoc task ->
      task.dependsOn delombokTask
      task.source = delombokTask.outputs
    }
  }

  /**
   * Adds JUnit dependency to specified source set configuration
   * @param sourceSet source set
   */
  void addJUnitDependency(SourceSet sourceSet) {
    project.dependencies.add("${ sourceSet.name }${ IMPLEMENTATION_CONFIGURATION_NAME.capitalize() }", [
      group: 'junit',
      name: 'junit',
      version: '[4.0,5.0)'
    ])
  }

  /**
   * Adds Spock to specified source set and task
   * @param sourceSet source set
   * @param task test task
   *        If null, task with the same name as source set is used
   */
  void addSpockDependency(SourceSet sourceSet, Test task = null) {
    addSpockDependency sourceSet, [project.tasks.withType(Test).getByName(sourceSet.name)], Closure.IDENTITY
  }

  /**
   * Adds Spock to specified source set and tasks
   * @param sourceSet source set
   * @param tasks list of test tasks.
   * @param reportDirNamer closure to set report directory name from task name
   */
  void addSpockDependency(SourceSet sourceSet, Iterable<Test> tasks, Closure<GString> reportDirNamer) {
    project.plugins.apply 'org.gradle.groovy'
    String sourceSetName = sourceSet.name
    project.dependencies.with {
      add("$sourceSetName${ IMPLEMENTATION_CONFIGURATION_NAME.capitalize() }", [
              group  : 'org.spockframework',
              name   : 'spock-core',
              version: "1.1-groovy-2.4" // ${ (GroovySystem.version =~ /^(\d+\.\d+)/).group(0) } // TODO: ???
      ]) { ModuleDependency dependency ->
        dependency.exclude(
                group: 'org.codehaus.groovy',
                module: 'groovy-all'
        )
      }
      add("$sourceSetName${ RUNTIME_ONLY_CONFIGURATION_NAME.capitalize() }", [
              group  : 'com.athaydes',
              name   : 'spock-reports',
              version: 'latest.release'
      ]) { ModuleDependency dependency ->
        dependency.transitive = false
      }
      add("$sourceSetName${ IMPLEMENTATION_CONFIGURATION_NAME.capitalize() }", [
              group  : 'org.slf4j',
              name   : 'slf4j-api',
              version: 'latest.release'
      ])
      add("$sourceSetName${ IMPLEMENTATION_CONFIGURATION_NAME.capitalize() }", [
              group  : 'org.slf4j',
              name   : 'slf4j-simple',
              version: 'latest.release'
      ])
    }

    tasks.each { Test task ->
      task.with {
        reports.html.enabled = false
        systemProperty 'com.athaydes.spockframework.report.outputDir', new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, "spock/${ toSafeFileName(reportDirNamer.call(name)) }").absolutePath
      }
    }

    new DslObject(project.tasks.getByName("codenarc${ sourceSetName.capitalize() }")).convention.getPlugin(CodeNarcTaskConvention).disabledRules.addAll(['MethodName', 'FactoryMethodName'])
  }

  /**
   * Configures integration test source set classpath
   * See https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests
   * @param sourceSet source set to configure
   * @return
   */
  void configureIntegrationTestSourceSetClasspath(SourceSet sourceSet) {
    sourceSet.compileClasspath += project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(MAIN_SOURCE_SET_NAME).output
    sourceSet.runtimeClasspath += sourceSet.output + sourceSet.compileClasspath

    // project.configurations["${ sourceSet.name }${ IMPLEMENTATION_CONFIGURATION_NAME.capitalize() }"].extendsFrom project.configurations['implementation']
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
        testClassesDirs = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME).output.classesDirs
        classpath = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME).runtimeClasspath
        reports.junitXml.setDestination new File(project.convention.getPlugin(ProjectConvention).xmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
        reports.html.setDestination new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
      }
    }

    addJUnitDependency functionalTestSourceSet
    addSpockDependency functionalTestSourceSet, functionalTestTask
  }

  private void configureArtifactory() {
    if (project.hasProperty('artifactoryUser') && project.hasProperty('artifactoryPassword')) {
      project.convention.getPlugin(ArtifactoryPluginConvention).clientConfig.publisher.with {
        repoKey = project.convention.getPlugin(ProjectConvention).isRelease ? 'libs-release-local' : 'libs-snapshot-local'
        username = project.property('artifactoryUser')
        password = project.property('artifactoryPassword')
        maven = true
        /*defaults {
            // publications
            publishConfigs 'archive' // Configurations
            // publications('mavenGroovy') // TODO
          }*/
      }
      project.tasks.getByName(RELEASE_TASK_NAME).finalizedBy project.tasks.withType(ArtifactoryTask)
      project.repositories.maven { MavenArtifactRepository mavenArtifactRepository ->
        mavenArtifactRepository.with {
          url = "$ARTIFACTORY_URL/${ project.convention.getPlugin(ProjectConvention).isRelease ? 'libs-release' : 'libs-snapshot' }/"
          credentials.username = project.property('artifactoryUser')
          credentials.password = project.property('artifactoryPassword')
        }
      }
    } else {
      project.repositories.with {
        jcenter()
        mavenCentral()
      }
    }
  }

  private void configureBintray() {
    project.plugins.apply 'com.jfrog.bintray'

    AnyLicenseInfo licenseInfo = project.convention.getPlugin(ProjectConvention).licenseInfo
    List<String> licenseList = new ArrayList<String>()
    if (licenseInfo instanceof LicenseSet) {
      for (AnyLicenseInfo license in ((LicenseSet)licenseInfo).members) {
        licenseList.add license.toString()
      }
    } else {
      licenseList.add licenseInfo.toString()
    }

    project.extensions.getByType(BintrayExtension).with {
      user = project.property('bintrayUser').toString()
      key = project.property('bintrayAPIKey').toString()
      pkg.repo = 'generic'
      pkg.name = 'gradle-project'
      pkg.userOrg = 'fidata'
      pkg.licenses = licenseList.toArray(new String[licenseList.size()])
      pkg.vcsUrl = project.convention.getPlugin(ProjectConvention).vcsUrl
      pkg.desc = '' // Version description
      pkg.version.name = ''
      pkg.version.vcsTag = '' // TODO
      pkg.version.gpg.sign = true // TODO ?
      // pkg.version.attributes // Attributes to be attached to the version
    }
    project.tasks.getByName(RELEASE_TASK_NAME).finalizedBy project.tasks.withType(BintrayPublishTask)
  }

  @PackageScope
  Map<String, GString> getJavadocLinks() {
    ['java': "https://docs.oracle.com/javase/${ (JavaVersion.toVersion(project.convention.getPlugin(JavaPluginConvention).targetCompatibility) ?: JavaVersion.current()).majorVersion }/docs/api/"]
  }

  private void configureJavadoc() {
    project.tasks.withType(Javadoc) { Javadoc task ->
      task.options { StandardJavadocDocletOptions options ->
        getJavadocLinks().values().each {
          options.links it
        }
      }
    }
  }

}
