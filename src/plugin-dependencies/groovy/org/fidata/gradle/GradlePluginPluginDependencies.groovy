#!/usr/bin/env groovy
/*
 * GradlePluginPluginDependencies class
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

import groovy.transform.CompileStatic

/**
 * List of dependencies of org.fidata.plugin plugin
 */
@CompileStatic
final class GradlePluginPluginDependencies {
  /**
   * List of plugin dependencies with IDs
   */
  static final Map<String, ? extends Map> PLUGIN_DEPENDENCIES = [
    'org.gradle.java-gradle-plugin': [:],
    'org.ajoberstar.stutter': [
      group: 'org.ajoberstar',
      name: 'gradle-stutter'
    ],
    'org.ysb33r.gradletest': [
      group: 'gradle.plugin.org.ysb33r.gradle',
      name: 'gradletest'
    ],
    'com.gradle.plugin-publish': [
      configurationName: 'implementation',
      group: 'com.gradle.publish',
      name: 'plugin-publish-plugin',
      enabled: false
    ],
  ]

  // Suppress default constructor for noninstantiability
  private GradlePluginPluginDependencies() {
    throw new AssertionError()
  }
}
