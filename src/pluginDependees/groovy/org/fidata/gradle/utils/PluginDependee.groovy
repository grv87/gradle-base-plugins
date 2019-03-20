#!/usr/bin/env groovy
/*
 * PluginDependee class
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
