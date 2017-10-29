#!/usr/bin/env groovy
/*
 * org.fidata.plugin Gradle plugin
 * Copyright © 2017  Basil Peace
 *
 * This file is part of gradle-fidata-plugin.
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

import static org.gradle.internal.FileUtils.toSafeFileName
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import org.gradle.api.tasks.testing.Test
// import org.gradle.api.publish.maven.MavenPublication

/**
 * Provide an environment for a Gradle plugin project
 */
class FIDATAGradlePluginPlugin implements Plugin<Project>, PropertyChangeListener {
  @Override
  void apply(Project project) {
    project.with {
      apply plugin: FIDATAGroovyProjectPlugin
      FIDATAGradlePluginPlugins.PLUGIN_IDS.each { plugins.apply it }

      fidata.addPropertyChangeListener this

      sourceSets {
        compatTest {
          compileClasspath += sourceSets.main.output + configurations.testRuntime
          runtimeClasspath += output + compileClasspath
        }
      }

      stutter {
        supports '4.2.1', '4.1'
      }

      tasks.withType(Test).matching { it.name.startsWith('compatTest') }.each { Test task ->
        task.with {
          String reportDirName = toSafeFileName((task.name - ~/^compatTest/).uncapitalize() /* Requires Groovy >= 2.4.8, i.e. Gradle >= 3.5 */)
          reports.with {
            html.enabled = false
            junitXml.destination = file("$xmlReportsDir/compatTest/$reportDirName")
          }
          systemProperty 'com.athaydes.spockframework.report.outputDir', "$htmlReportsDir/spock/compatTest/$reportDirName"
        }
      }

      artifactory {
        publish {
          repository {
            if (isRelease) {
              repoKey = 'plugins-release-local'
            }
            else {
              repoKey = 'plugins-snapshot-local'
            }
          }
        }
      }
    }
  }

  void propertyChange(PropertyChangeEvent e) {
    if ((e.source == project.fidata) && (e.propertyName == 'publicReleases')) {
      project.with {
        if (fidata.publicReleases) {
          plugins.apply 'com.gradle.plugin-publish'
          pluginBundle {
            website = "https://github.com/FIDATA/${name}"
            vcsUrl = "https://github.com/FIDATA/${name}"
            mavenCoordinates {
              groupId = group
            }
          }
          if (isRelease) {
            tasks.release.finalizedBy publishPlugins
          }
        }
      }
    }
  }
}
