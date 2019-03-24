#!/usr/bin/env groovy
/*
 * PluginDependeesUtils class
 * Copyright © 2017-2018  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
    pluginDependees.findAll { String key, PluginDependee value ->
      value.enabled &&
        (!isBuildSrc || value.enabledForBuildSrc) &&
        (project == project.rootProject || value.enabledForSubprojects)
    }.keySet().each { String id ->
      project.pluginManager.apply id
    }
  }

  // Suppress default constructor for noninstantiability
  private PluginDependeesUtils() {
    throw new UnsupportedOperationException()
  }
}
