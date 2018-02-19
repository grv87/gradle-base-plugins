#!/usr/bin/env groovy
/*
 * ProjectPluginDependencies class
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

import groovy.transform.CompileStatic

/**
 * List of dependencies of org.fidata.project plugin
 */
@CompileStatic
final class ProjectPluginDependencies {
  /*
   * BLOCKED: https://github.com/gradle/gradle/issues/1050
   * Some plugins are published to Gradle Plugins portal only
   * and don't exist in JCenter or Maven Central.
   * Gradle Plugins portal doesn't provide maven-metadata,
   * so Gradle can't detect latest version.
   * We have to provide specific versions for such plugins <>
   */
  /**
   * List of plugin dependencies with IDs
   */
  static final Map<String, ? extends Map> PLUGIN_DEPENDENCIES = [
    'org.gradle.eclipse': [:],
    'org.gradle.lifecycle-base': [:],
    'nebula.contacts': [
      group: 'com.netflix.nebula',
      name: 'gradle-contacts-plugin'
    ],
    'nebula.dependency-lock': [
      configurationName: 'implementation',
      group: 'com.netflix.nebula',
      name: 'gradle-dependency-lock-plugin',
      excludes: [
        [
          group: 'xerces',
          module: 'xercesImpl'
        ],
      ]
    ],
    'com.github.ben-manes.versions': [
      configurationName: 'implementation',
      group: 'com.github.ben-manes',
      name: 'gradle-versions-plugin'
    ],
    'com.jfrog.artifactory': [
      configurationName: 'implementation',
      group: 'org.jfrog.buildinfo',
      name: 'build-info-extractor-gradle',
      excludes: [
        [
          group: 'org.codehaus.groovy',
          module: 'groovy-all'
        ],
      ]
    ],
    'org.gradle.signing': [:],
    'de.gliderpilot.semantic-release': [
      configurationName: 'implementation',
      group: 'de.gliderpilot.gradle.semantic-release',
      name: 'gradle-semantic-release-plugin'
    ],
    'org.ajoberstar.git-publish': [
      configurationName: 'implementation',
      group: 'org.ajoberstar',
      name: 'gradle-git-publish'
    ],
    'org.gradle.reporting-base': [:],
    'org.gradle.codenarc': [:],
    'org.gradle.project-report': [:],
    'cz.malohlava': [
      configurationName: 'implementation',
      group: 'cz.malohlava',
      name: 'visteg'
    ],
    'com.dorongold.task-tree': [
      group: 'gradle.plugin.com.dorongold.plugins',
      name: 'task-tree',
      version: '1.3'
    ],
  ]

  // Suppress default constructor for noninstantiability
  private ProjectPluginDependencies() {
    throw new AssertionError()
  }
}