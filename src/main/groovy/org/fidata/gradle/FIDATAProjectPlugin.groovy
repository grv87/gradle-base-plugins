#!/usr/bin/env groovy
/*
 * org.fidata.project Gradle plugin
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
import com.github.zafarkhaja.semver.Version
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.tasks.testing.Test

/**
 * Provide an environment for a project
 */
class FIDATAProjectPlugin implements Plugin<Project> {
  @Override
  @SuppressWarnings('CouldBeElvis')
  void apply(Project project) {
    project.with {
      extensions.create('fidata', FIDATAExtension)
      FIDATAProjectPlugins.PLUGIN_IDS.each { plugins.apply it }

      if (!group) { group = 'org.fidata' }

      /*
       * CAVEAT: https://github.com/tschulte/gradle-semantic-release-plugin/issues/27
       * We have to call version.toString() to initialize version.inferredVersion <>
       */
      ext.isRelease = !version.toString().endsWith('-SNAPSHOT')
      ext.changeLog = semanticRelease.changeLog.changeLog(semanticRelease.changeLog.commits(Version.valueOf(project.version.inferredVersion.previousVersion)), project.version.inferredVersion)

      /*sourceSets {
        buildSrc {
          groovy {
            srcDir 'buildSrc'
            include 'build.gradle'
            include 'settings.gradle'
            include 'Jenkinsfile'
            include 'config/codenarc/codenarc.groovy'
          }
        }
      }*/

      if (hasProperty('artifactoryUser') && hasProperty('artifactoryPassword')) {
        artifactory {
          contextUrl = 'https://fidata.jfrog.io/fidata'
          resolve {
            repository {
              if (isRelease) { // TODO
                repoKey = 'libs-release'
              }
              else {
                repoKey = 'libs-snapshot'
              }
              username = getProperty('artifactoryUser')
              password = getProperty('artifactoryPassword')
              maven = true
            }
          }
          publish {
          }
        }
      }
      else {
        repositories { jcenter() }
      }

      task('wrapper', type: Wrapper) {
        group 'Chore'
        gradleVersion = '4.2.1'
      }

      dependencyUpdates.outputFormatter = 'xml'

      ext.with { // TODO
        gitUsername = hasProperty('gitUsername') ? getProperty('gitUsername') : System.getenv('GIT_USER') ?: System.getenv('GITCREDENTIALUSERNAME') ?: null
        gitPassword = hasProperty('gitPassword') ? getProperty('gitPassword') : System.getenv('GIT_PASS') ?: System.getenv('GITCREDENTIALPASSWORD') ?: null
        if (gitUsername && gitPassword) {
          System.with {
            setProperty('org.ajoberstar.grgit.auth.username', gitUsername)
            setProperty('org.ajoberstar.grgit.auth.password', gitPassword)
          }
        }
      }

      semanticRelease {
        repo {
          ghToken = hasProperty('ghToken') ? getProperty('ghToken') : System.getenv('GH_TOKEN') ?: null
        }
      }

      ext.with {
        reportsDir = file('build/reports')
        xmlReportsDir = file("$reportsDir/xml")
        htmlReportsDir = file("$reportsDir/html")
      }

      /*afterEvaluate { Project project ->
        if (!project.tasks.findByName('check')) {
          project.task('check')
        }
      }*/

      task('codenarcBuildSrc', type: CodeNarc) {
        for (f in new FileNameFinder().getFileNames('.', '**/*.gradle')) {
          source f
        }
        for (f in new FileNameFinder().getFileNames('buildSrc', '**/*.groovy **/*.gradle')) {
          source f
        }
        source 'Jenkinsfile'
        source 'config/codenarc/codenarc.groovy' // TODO
      }
      afterEvaluate { Project evaluatedProject ->
        evaluatedProject.tasks.withType(CodeNarc).each { CodeNarc task ->
          task.with {
            configFile = file('config/codenarc/codenarc.groovy')
            String reportFileName = toSafeFileName((task.name - ~/^codenarc/).uncapitalize())
            reports {
              xml {
                enabled = true
                destination = file("$xmlReportsDir/codenarc/${reportFileName}.xml")
              }
              // if CodeNarcReports. // TODO
              console.enabled = true
              html {
                enabled = true
                destination = file("$htmlReportsDir/codenarc/${reportFileName}.html")
              }
            }
            evaluatedProject.tasks.check.dependsOn task
          }
        }
      }

      afterEvaluate { Project evaluatedProject ->
        evaluatedProject.tasks.withType(Test).each { Test task ->
          evaluatedProject.tasks.check.dependsOn task
        }
      }
      // tasks.release.dependsOn tasks.check


      // task('generateProjectFiles')
    }
  }
}
