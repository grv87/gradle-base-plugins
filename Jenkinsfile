#!/usr/bin/env groovy
/*
 * Jenkinsfile for gradle-base-plugins
 * Copyright © 2018  Basil Peace
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
node {
  Object rtGradle

  stage ('Checkout') {
    checkout scm
  }

  stage ('Artifactory configuration') {
    Artifactory.server 'FIDATA'
    rtGradle = Artifactory.newGradleBuild()
    rtGradle.useWrapper = true
    rtGradle.usesPlugin = true
  }

  withCredentials([
    usernamePassword(credentialsId: 'Github 2', usernameVariable: 'ORG_GRADLE_PROJECT_gitUsername', passwordVariable: 'ORG_GRADLE_PROJECT_gitPassword'),
    string(credentialsId: 'Github', variable: 'ORG_GRADLE_PROJECT_ghToken'),
    usernamePassword(credentialsId: 'Artifactory', usernameVariable: 'ORG_GRADLE_PROJECT_artifactoryUser', passwordVariable: 'ORG_GRADLE_PROJECT_artifactoryPassword'),
  ]) {
    stage ('Clean') {
      rtGradle.run tasks: 'clean', switches: '--full-stacktrace'
    }
    stage ('Assemble') {
      rtGradle.run tasks: 'assemble', switches: '--full-stacktrace'
    }
    stage ('Check') {
      // rtGradle.run tasks: 'check', switches: '--full-stacktrace'
      rtGradle.run tasks: 'compatTest', switches: '--full-stacktrace'
    }
    stage ('Release') {
      rtGradle.run tasks: 'release', switches: '--full-stacktrace'
    }
  }
}
