#!/usr/bin/env groovy
/*
 * org.fidata.project.groovy Gradle plugin
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

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test

/**
 * Provide an environment for a Groovy project
 */
class FIDATAGroovyProjectPlugin implements Plugin<Project>, PropertyChangeListener {
  @Override
  void apply(Project project) {
    project.with {
      apply plugin: FIDATAProjectPlugin
      for (String id in FIDATAGroovyProjectPlugins.PLUGIN_IDS) {
        plugins.apply id
      }
      // FIDATAGroovyProjectPlugins.PLUGIN_IDS.each { plugins.apply it }

      fidata.addPropertyChangeListener this

      dependencies {
        String groovyVersion = GroovySystem.version
        compile(
          group: 'org.codehaus.groovy',
          name: 'groovy-all',
          version: groovyVersion
        )
        compile(gradleApi())
        testCompile(gradleTestKit())
        testCompile(
          group: 'junit',
          name: 'junit',
          version: 'latest.release'
        )
        testCompile(
          group: 'org.spockframework',
          name: 'spock-core',
          version: "1.1-groovy-${(groovyVersion =~ /^(\d+\.\d+)/)[0][0]}",
        ) {
          exclude(
            group: 'org.codehaus.groovy',
            module: 'groovy-all'
          )
        }
        testRuntime(
          group: 'com.athaydes',
          name: 'spock-reports',
          version: 'latest.release'
        ) {
          transitive = false
        }
        testCompile(
          group: 'org.slf4j',
          name: 'slf4j-api',
          version: 'latest.release'
        )
        testCompile(
          group: 'org.slf4j',
          name: 'slf4j-simple',
          version: 'latest.release'
        )
      }

      ext.javaVersion = 1.8 // TODO
      sourceCompatibility = javaVersion
      targetCompatibility = javaVersion
      tasks.withType(JavaCompile) { task ->
        task.sourceCompatibility = sourceCompatibility
        task.targetCompatibility = targetCompatibility
      }
      tasks.withType(GroovyCompile) { task ->
        task.sourceCompatibility = sourceCompatibility
        task.targetCompatibility = targetCompatibility
      }

      /*signing.with { // TODO
        keyId = 24875D73
        signing.password = secret
        signing.secretKeyRingFile = /Users/me/.gnupg/secring.gpg
      }*/

      sourceSets {
        functionalTest {
          groovy {
              srcDir file('src/functionalTest/groovy')
          }
          resources {
              srcDir file('src/functionalTest/resources')
          }
          compileClasspath += sourceSets.main.output + configurations.testRuntime
          runtimeClasspath += output + compileClasspath
        }
      }

      task('functionalTest', type: Test) {
        testClassesDirs = sourceSets.functionalTest.output.classesDirs
        classpath = sourceSets.functionalTest.runtimeClasspath
        reports.with {
          junitXml.destination = file("$xmlReportsDir/functionalTest")
          html.enabled = false
        }
        systemProperty 'com.athaydes.spockframework.report.outputDir', "$htmlReportsDir/spock/functionalTest"
      }

      configurations {
        deployerJars
      }

      /*signing {
        sign configurations.deployerJars
      }*/

      /*dependencies {
        deployerJars 'org.apache.maven.wagon:wagon-ssh:2.2'
        http org.apache.maven.wagon:wagon-http:2.2
        ssh org.apache.maven.wagon:wagon-ssh:2.2
        ssh-external org.apache.maven.wagon:wagon-ssh-external:2.2
        ftp org.apache.maven.wagon:wagon-ftp:2.2
        webdav org.apache.maven.wagon:wagon-webdav:1.0-beta-2
        file -
      }*/

      uploadArchives {
        repositories {
          mavenDeployer {
            // beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }
          }
        }
      } // TODO

      if (hasProperty('artifactoryUser') && hasProperty('artifactoryPassword')) {
        artifactory {
          publish {
            repository {
              if (isRelease) {
                repoKey = 'libs-release-local'
              }
              else {
                repoKey = 'libs-snapshot-local'
              }
              username = getProperty('artifactoryUser')
              password = getProperty('artifactoryPassword')
              maven = true
            }
            defaults {
              // publications
              publishConfigs 'archive' // Configurations
              // publications('mavenGroovy') // TODO
            }
          }
        }
        tasks.release.finalizedBy artifactoryPublish
      }
    }
  }

  void propertyChange(PropertyChangeEvent e) {
    if ((e.source == project.fidata) && (e.propertyName == 'publicReleases')) {
      project.with {
        bintray {
          user = 'bintray_user'
          key = 'bintray_api_key'
          pkg {
              repo = 'generic'
              name = 'gradle-project'
              userOrg = 'bintray_user'
              licenses = ['Apache-2.0']
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
