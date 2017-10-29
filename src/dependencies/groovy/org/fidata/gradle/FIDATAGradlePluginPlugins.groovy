#!/usr/bin/env groovy
/*
 * FIDATAGradlePluginPlugins class
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
 * List of plugins applied by org.fidata.plugin plugin
 */
final class FIDATAGradlePluginPlugins {
  static final Map<String, Map> GRADLE_PLUGINS = [
    'java-gradle-plugin': [:],
  ]

  /*
   * BLOCKED: https://github.com/gradle/gradle/issues/1050
   * Some plugins are published to Gradle Plugins portal and doesn't exist in JCenter.
   * Gradle Plugins portal doesn't provide maven-metadata, so Gradle can't detect latest version.
   * We have to provide speficic versions for such plugins <>
   */
  static final Map<String, Map> THIRDPARTY_PLUGINS = [
    'org.ajoberstar.stutter': [
      group: 'org.ajoberstar',
      name: 'gradle-stutter',
    ],
    'com.gradle.plugin-publish': [
      group: 'com.gradle.publish',
      name: 'plugin-publish-plugin',
      version: '0.9.9',
      enabled: false
    ],
  ]

  static final List PLUGIN_IDS = GRADLE_PLUGINS.findAll({ it.value.get('enabled', true) })*.key + THIRDPARTY_PLUGINS.findAll({ it.value.get('enabled', true) })*.key

  // Suppress default constructor for noninstantiability
  private FIDATAGradlePluginPlugins() {
    throw new AssertionError()
  }
}
