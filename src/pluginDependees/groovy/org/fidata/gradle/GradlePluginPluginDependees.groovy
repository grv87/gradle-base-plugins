#!/usr/bin/env groovy
/*
 * GradlePluginPluginDependees class
 * Copyright ©  Basil Peace
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
package org.fidata.gradle

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import groovy.transform.Internal
import org.fidata.gradle.utils.PluginDependee

/**
 * List of dependees of org.fidata.plugin plugin
 */
@Internal
@CompileStatic
final class GradlePluginPluginDependees {
  /**
   * List of plugin dependees with IDs
   */
  static final Map<String, PluginDependee> PLUGIN_DEPENDEES = ImmutableMap.copyOf([
    'org.gradle.java-gradle-plugin': new PluginDependee(),
    'org.ajoberstar.stutter': new PluginDependee(
      version: '[0, 1[',
    ),
    'org.ysb33r.gradletest': new PluginDependee(
      configurationName: 'implementation',
      version: '[2, 3[',
      status: 'milestone',
    ),
    'com.gradle.plugin-publish': new PluginDependee(
      configurationName: 'implementation',
      version: '[0, 1[',
      enabled: false,
    ),
  ])

  // Suppress default constructor for noninstantiability
  private GradlePluginPluginDependees() {
    throw new UnsupportedOperationException()
  }
}
