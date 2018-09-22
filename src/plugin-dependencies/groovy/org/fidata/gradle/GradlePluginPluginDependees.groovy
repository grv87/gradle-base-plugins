#!/usr/bin/env groovy
/*
 * GradlePluginPluginDependees class
 * Copyright © 2017-2018  Basil Peace
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
package org.fidata.gradle

import groovy.transform.CompileStatic
import org.fidata.gradle.utils.PluginDependee

/**
 * List of dependees of org.fidata.plugin plugin
 */
@CompileStatic
final class GradlePluginPluginDependees {
  /**
   * List of plugin dependees with IDs
   */
  static final Map<String, PluginDependee> PLUGIN_DEPENDEES = [
    'org.gradle.java-gradle-plugin': new PluginDependee(),
    'org.ajoberstar.stutter': new PluginDependee(
      group: 'org.ajoberstar',
      module: 'gradle-stutter',
    ),
    'org.ysb33r.gradletest': new PluginDependee(
      configurationName: 'implementation',
      group: 'gradle.plugin.org.ysb33r.gradle',
      module: 'gradletest',
    ),
    'com.gradle.plugin-publish': new PluginDependee(
      configurationName: 'implementation',
      group: 'com.gradle.publish',
      module: 'plugin-publish-plugin',
      enabled: false
    ),
  ]

  // Suppress default constructor for noninstantiability
  private GradlePluginPluginDependees() {
    throw new UnsupportedOperationException()
  }
}
