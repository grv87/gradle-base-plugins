#!/usr/bin/env groovy
/*
 * JavaProjectPluginDependees class
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
 * List of dependees of org.fidata.project.java plugin
 */
@CompileStatic
final class JavaProjectPluginDependees {
  /**
   * List of plugin dependees with IDs
   */
  static final Map<String, PluginDependee> PLUGIN_DEPENDEES = [
    'org.gradle.java': new PluginDependee(),
    'io.franzbecker.gradle-lombok': new PluginDependee(
      configurationName: 'implementation',
      group: 'io.franzbecker',
      module: 'gradle-lombok',
    ),
  ]

  // Suppress default constructor for noninstantiability
  private JavaProjectPluginDependees() {
    throw new UnsupportedOperationException()
  }
}
