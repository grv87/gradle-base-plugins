#!/usr/bin/env groovy
/*
 * JDKProjectPluginDependencies class
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
package org.fidata.gradle

import groovy.transform.CompileStatic

/**
 * List of dependencies of org.fidata.project.jdk plugin
 */
@CompileStatic
final class JDKProjectPluginDependencies {
  /**
   * List of plugin dependencies with IDs
   */
  static final Map<String, ? extends Map> PLUGIN_DEPENDENCIES = [
    'org.gradle.java': [:],
    'org.gradle.java-library': [:],
    'org.gradle.maven': [:],
    'com.jfrog.bintray': [
      group: 'com.jfrog.bintray.gradle',
      name: 'gradle-bintray-plugin',
      excludes: [
        [
          group: 'xerces',
          module: 'xercesImpl'
        ],
      ],
      enabled: false
    ],
  ]

  /**
   * List of IDs of plugins enabled by default
   */
  static final List<String> DEFAULT_PLUGINS = PLUGIN_DEPENDENCIES.findAll { it.value.get('enabled', true) } *.key

  /**
   * List of non-plugin dependencies
   */
  static final List<? extends Map> NON_PLUGIN_DEPENDENCIES = []

  /**
   * Total list of dependencies
   */
  static final List<? extends Map> DEPENDENCIES = PLUGIN_DEPENDENCIES.findAll { !it.key.startsWith('org.gradle.') }.collect { [configurationName: 'implementation'] + it.value } + NON_PLUGIN_DEPENDENCIES

  // Suppress default constructor for noninstantiability
  private JDKProjectPluginDependencies() {
    throw new AssertionError()
  }
}
