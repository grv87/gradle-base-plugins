#!/usr/bin/env groovy
/*
 * FIDATAProjectPlugins class
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

/**
 * List of plugins applied by org.fidata.project plugin
 */
final class FIDATAProjectPlugins {
  static final Map<String, Map> GRADLE_PLUGINS = [
    'base': [:],
    'codenarc': [:],
  ]

  /*
   * BLOCKED: https://github.com/gradle/gradle/issues/1050
   * Some plugins are published to Gradle Plugins portal and doesn't exist in JCenter.
   * Gradle Plugins portal doesn't provide maven-metadata, so Gradle can't detect latest version.
   * We have to provide speficic versions for such plugins <>
   */
  static final Map<String, Map> THIRDPARTY_PLUGINS = [
    'nebula.contacts': [
      group: 'com.netflix.nebula',
      name: 'gradle-contacts-plugin'
    ],
    'com.jfrog.artifactory': [
      group: 'org.jfrog.buildinfo',
      name: 'build-info-extractor-gradle',
      excludes: [
        [
          group: 'org.codehaus.groovy',
          module: 'groovy-all'
        ],
      ]
    ],
    'nebula.dependency-lock': [
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
      group: 'com.github.ben-manes',
      name: 'gradle-versions-plugin'
    ],
    'de.gliderpilot.semantic-release': [
       group: 'de.gliderpilot.gradle.semantic-release',
       name: 'gradle-semantic-release-plugin'
    ],
  ]

  static final List PLUGIN_IDS = GRADLE_PLUGINS.findAll({ it.value.get('enabled', true) })*.key + THIRDPARTY_PLUGINS.findAll({ it.value.get('enabled', true) })*.key

  // Suppress default constructor for noninstantiability
  private FIDATAProjectPlugins() {
    throw new AssertionError()
  }
}
