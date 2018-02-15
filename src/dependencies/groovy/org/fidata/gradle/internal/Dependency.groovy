#!/usr/bin/env groovy
/*
 * GroovyProjectPluginDependencies class
 * Copyright © 2017  Basil Peace
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
package org.fidata.gradle.internal

import groovy.transform.CompileStatic

/**
 * Dependency
 */
@CompileStatic
class Dependency {
  /**
   * List of plugin dependencies with IDs
   */
  static final Map<String, ? extends Map> PLUGIN_DEPENDENCIES = [
	'org.gradle.groovy': [:],
  ]

  /**
   * List of IDs of plugins enabled by default
   */
  static final List<String> DEFAULT_PLUGINS = PLUGIN_DEPENDENCIES.findAll { it.value.get('enabled', true) } *.key

  /**
   * Groovy version
   */
  static final String GROOVY_VERSION = GroovySystem.version

  /**
   * List of non-plugin dependencies
   */
  static final List<? extends Map> NON_PLUGIN_DEPENDENCIES = [
	[
	  configurationName: 'api',
	  group: 'org.codehaus.groovy',
	  name: 'groovy-all',
	  version: GROOVY_VERSION
	],
  ]

  /**
   * Total list of dependencies
   */
  static final List<? extends Map> DEPENDENCIES = PLUGIN_DEPENDENCIES.findAll { !it.key.startsWith('org.gradle.') }.collect { [configurationName: 'implementation'] + it.value } + NON_PLUGIN_DEPENDENCIES
}