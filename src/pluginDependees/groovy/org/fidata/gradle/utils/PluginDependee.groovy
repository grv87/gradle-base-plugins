#!/usr/bin/env groovy
/*
 * PluginDependee class
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

/**
 * Plugin dependee
 */
@CompileStatic
final class PluginDependee {
  /**
   * Configuration name.
   * By default, it is runtimeOnly
   */
  String configurationName = 'runtimeOnly'
  /**
   * Group name
   */
  String group
  /**
   * Module name
   */
  String module
  /**
   * Version.
   * Should be specified for non-built-in plugins
   */
  String version
  /**
   * Status.
   * Should be used in combination with version ranges to manually include pre-release artifacts,
   * i.e. {@code status = 'integration'}
   */
  String status
  /**
   * List of exclusions
   */
  Set<PluginDependeeExclusion> excludes = null
  /**
   * Whether this dependee is enabled during default applicating.
   * True by default
   */
  boolean enabled = true
  /**
   * Whether this dependee is enabled during default applicating
   * for buildSrc project.
   * True by default.
   *
   * Makes no difference if enabled = false
   */
  boolean enabledForBuildSrc = true
  /**
   * Whether this dependee is enabled during default applicating
   * for subprojects.
   * True by default.
   *
   * Makes no difference if enabled = false
   */
  boolean enabledForSubprojects = true
}
