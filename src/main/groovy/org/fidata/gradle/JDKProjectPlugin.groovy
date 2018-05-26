#!/usr/bin/env groovy
/*
 * org.fidata.project.jdk Gradle plugin
 * Copyright © 2017  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import static JDKProjectPluginDependencies.PLUGIN_DEPENDENCIES
import static ProjectPlugin.LICENSE_FILE_NAMES
import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.reporting.ReportingExtension
// import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask // TODO
// import org.ajoberstar.gradle.git.publish.GitPublishExtension
import org.ajoberstar.gradle.git.ghpages.GithubPagesPluginExtension
import org.ajoberstar.gradle.git.ghpages.GithubPagesPlugin

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JDKProjectPlugin extends AbstractPlugin implements PropertyChangeListener {
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

  @Override
  void apply(Project project) {
    super.apply(project)
    project.with {
      plugins.with {
        apply ProjectPlugin

        PLUGIN_DEPENDENCIES.findAll() { Map.Entry<String, ? extends Map> depNotation -> depNotation.value.getOrDefault('enabled', true) }.keySet().each { String id ->
          apply id
        }
      }

      convention.getPlugin(ProjectConvention).addPropertyChangeListener this
      configurePublicReleases()

      JDKExtension jdkExtension = new JDKExtension()
      project.extensions.add 'jdk', jdkExtension
      jdkExtension.addPropertyChangeListener this
      configureJDKSourceVersion()
      configureJDKTargetVersion()
      configurePublicReleases()

      tasks.withType(ProcessResources) { ProcessResources task ->
        task.from(LICENSE_FILE_NAMES).into 'META-INF'
      }

      dependencies.add('testImplementation', [
        group: 'junit',
        name: 'junit',
        version: 'latest.release'
      ])

    }

    // project.convention.getPlugin(JavaPluginConvention).with {
      project.convention.getPlugin(JavaPluginConvention).testReportDirName = project.extensions.getByType(ReportingExtension).baseDir.toPath().relativize(new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, 'tests').toPath()).toString() // TODO: ???

      project.convention.getPlugin(JavaPluginConvention).sourceSets.create(FUNCTIONAL_TEST_SOURCE_SET_NAME) { SourceSet sourceSet ->
        // sourceSet.with {
          sourceSet.java.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/java")
          sourceSet.resources.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/resources")
          sourceSet.compileClasspath += project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName('main').output + project.configurations.getByName('testCompileClasspath')
          sourceSet.runtimeClasspath += sourceSet.output + sourceSet.compileClasspath + project.configurations.getByName('testRuntimeClasspath')
        // }
      }
    // }

    project.with {
      task(FUNCTIONAL_TEST_TASK_NAME, type: Test) { Test task ->
        task.with {
          group = 'Verification'
          description = 'Runs functional tests'
          testClassesDirs = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME).output.classesDirs
          classpath = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(FUNCTIONAL_TEST_SOURCE_SET_NAME).runtimeClasspath
          reports.with {
            junitXml.setDestination new File(project.convention.getPlugin(ProjectConvention).xmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
            html.setDestination new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
          }
        }
      }

      configurations.create('deployerJars')

      /*signing {
        sign configurations.getByName('deployerJars')
      }*/

      dependencies.add('deployerJars', [
        /*deployerJars 'org.apache.maven.wagon:wagon-ssh:2.2'
        http org.apache.maven.wagon:wagon-http:2.2*/
        group: 'org.apache.maven.wagon',
        name: 'wagon-ssh',
        version: 'latest.release'
        // ssh
       /* ssh-external org.apache.maven.wagon:wagon-ssh-external:2.2
        ftp org.apache.maven.wagon:wagon-ftp:2.2
        webdav org.apache.maven.wagon:wagon-webdav:1.0-beta-2
        file -*/
      ])

      /*uploadArchives {
        repositories {
          mavenDeployer {
            // beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }
          }
        }
      } // TODO*/

      configureArtifactory()

      // extensions.getByType(GitPublishExtension).contents.from(tasks.getByName('javadoc')).into "$version/javadoc"
      extensions.getByType(GithubPagesPluginExtension).pages.from(tasks.getByName('javadoc')).into "$version/javadoc"
    }
  }

  /**
   * Gets called when a property is changed
   */
  void propertyChange(PropertyChangeEvent e) {
    // TODO: Switch on class ?
    switch (e.source) {
      case project.convention.getPlugin(ProjectConvention):
        switch (e.propertyName) {
          case 'publicReleases':
            configurePublicReleases()
            break
        }
        break
      case project.extensions.getByType(JDKExtension):
        switch (e.propertyName) {
          case 'sourceVersion':
            configureJDKSourceVersion()
            break
          case 'targetVersion':
            configureJDKTargetVersion()
            break
        }
        break
    }
  }

  private void configureJDKSourceVersion() {
    project.with {
      convention.getPlugin(JavaPluginConvention).sourceCompatibility = extensions.getByType(JDKExtension).sourceVersion
    }
  }

  private void configureJDKTargetVersion() {
    project.with {
      convention.getPlugin(JavaPluginConvention).targetCompatibility = extensions.getByType(JDKExtension).targetVersion
    }
  }

  @CompileDynamic
  private void configurePublicReleases() {
    if (project.convention.getPlugin(ProjectConvention).publicReleases) {
      configureBintray()
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
          publish {
            repository {
              repoKey = convention.getPlugin(ProjectConvention).isRelease ? 'libs-release-local' : 'libs-snapshot-local'
              username = project.getProperty('artifactoryUser')
              password = project.getProperty('artifactoryPassword')
              maven = true
            }
            defaults {
              // publications
              publishConfigs 'archive' // Configurations
              // publications('mavenGroovy') // TODO
            }
          }
        }
        tasks.getByName('release').finalizedBy tasks.getByName(/*BuildInfoBaseTask.BUILD_INFO_TASK_NAME*/ 'artifactoryPublish') // TODO
      }
    }
  }

  @CompileDynamic
  private void configureBintray() {
    project.with {
      plugins.apply 'com.jfrog.bintray'
      bintray {
        user = 'bintray_user'
        key = 'bintray_api_key'
        pkg {
          repo = 'generic'
          name = 'gradle-project'
          userOrg = 'bintray_user'
          licenses = ['Apache-2.0'] // TODO
          vcsUrl = convention.getPlugin(ProjectConvention).vcsUrl
          desc = '' // Version description
          vcsTag = ''
          attributes //  Attributes to be attached to the version
        }
      }
    }
  }
}
