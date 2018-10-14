#!/usr/bin/env groovy
/*
 * PluginDependeesUtils class
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
package org.fidata.gradle.utils

import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Class to handle list of plugin dependees
 */
@CompileStatic
final class PluginDependeesUtils {
  /**
   * Apply list of plugin dependees to the project
   * @param project project
   * @param isBuildSrc whether this project is buildSrc project.
   *        This method doesn't contain algorithm to determine it
   *        and relies on the information from the caller
   * @param pluginDependees list of plugin dependees
   */
  static final void applyPlugins(Project project, boolean isBuildSrc, Map<String, PluginDependee> pluginDependees) {
    pluginDependees.findAll { String key, PluginDependee value -> value.enabled && !isBuildSrc || value.enabledForBuildSrc }.keySet().each { String id ->
      project.pluginManager.apply id
    }
  }

  // Suppress default constructor for noninstantiability
  private PluginDependeesUtils() {
    throw new UnsupportedOperationException()
  }
}
