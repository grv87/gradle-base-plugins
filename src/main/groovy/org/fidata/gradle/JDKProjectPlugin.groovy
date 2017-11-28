#!/usr/bin/env groovy
/*
 * org.fidata.project.jdk Gradle plugin
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

import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent

/**
 * Provides an environment for a JDK project
 */
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
      apply plugin: ProjectPlugin
      for (String id in JDKProjectPluginDependencies.DEFAULT_PLUGINS) {
        plugins.apply id
      }

      convention.getPlugin(ProjectConvention).addPropertyChangeListener this
      configurePublicReleases()

      extensions.create('jdk', JDKExtension)
      jdk.addPropertyChangeListener this
      configureJDKSourceVersion()
      configureJDKTargetVersion()
      configurePublicReleases()

      tasks.withType(ProcessResources) { ProcessResources task ->
        task.with {
          from(ProjectPlugin.LICENSE_FILE_NAMES) {
            into 'META-INF'
          }
        }
      }

      dependencies {
        testImplementation(
          group: 'junit',
          name: 'junit',
          version: 'latest.release'
        )
      }

      testReportDirName = reporting.baseDir.toPath().relativize(new File(htmlReportsDir, 'tests').toPath()).toString()

      sourceSets {
        create(FUNCTIONAL_TEST_SOURCE_SET_NAME) {
          java.srcDir file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/java")
          resources.srcDir file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/resources")
          compileClasspath += sourceSets.main.output + configurations.testCompileClasspath
          runtimeClasspath += output + compileClasspath + configurations.testRuntimeClasspath
        }
      }

      task(FUNCTIONAL_TEST_TASK_NAME, type: Test) {
        group 'Verification'
        description 'Runs functional tests'
        testClassesDirs = sourceSets[FUNCTIONAL_TEST_SOURCE_SET_NAME].output.classesDirs
        classpath = sourceSets[FUNCTIONAL_TEST_SOURCE_SET_NAME].runtimeClasspath
        reports.with {
          junitXml.destination = new File(xmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
          html.destination = new File(htmlReportsDir, FUNCTIONAL_TEST_REPORTS_DIR_NAME)
        }
      }

      configurations {
        deployerJars
      }

      /*signing {
        sign configurations.deployerJars
      }*/

      dependencies {
        /*deployerJars 'org.apache.maven.wagon:wagon-ssh:2.2'
        http org.apache.maven.wagon:wagon-http:2.2*/
        deployerJars(
          group: 'org.apache.maven.wagon',
          name: 'wagon-ssh',
          version: 'latest.release'
        )// ssh
       /* ssh-external org.apache.maven.wagon:wagon-ssh-external:2.2
        ftp org.apache.maven.wagon:wagon-ftp:2.2
        webdav org.apache.maven.wagon:wagon-webdav:1.0-beta-2
        file -*/
      }

      uploadArchives {
        repositories {
          mavenDeployer {
            // beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }
          }
        }
      } // TODO

      if (project.hasProperty('artifactoryUser') && project.hasProperty('artifactoryPassword')) {
        artifactory {
          publish {
            repository {
              repoKey = isRelease ? 'libs-release-local' : 'libs-snapshot-local'
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
        tasks.release.finalizedBy artifactoryPublish // TODO
      }

      gitPublish.contents {
        from(javadoc) {
          into "$version/javadoc"
        }
      }
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
      case project.jdk:
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
      sourceCompatibility = jdk.sourceVersion
    }
  }

  private void configureJDKTargetVersion() {
    project.with {
      targetCompatibility = jdk.targetVersion
    }
  }

  private void configurePublicReleases() {
    project.with {
      if (publicReleases) {
        plugins.apply 'com.jfrog.bintray'
        bintray {
          user = 'bintray_user'
          key = 'bintray_api_key'
          pkg {
            repo = 'generic'
            name = 'gradle-project'
            userOrg = 'bintray_user'
            licenses = ['Apache-2.0'] // TODO
            vcsUrl = 'https://github.com/bintray/gradle-bintray-plugin.git'
            desc = '' // Version description
            vcsTag = ''
            attributes //  Attributes to be attached to the version
          }
        }
      }
    }
  }
}
