#!/usr/bin/env groovy
/*
 * FIDATAGroovyProjectPlugins class
 * Copyright © 2017  Basil Peace
 *
 * This file is part of gradle-fidata-plugin.
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

/**
 * List of plugins applied by org.fidata.project.groovy plugin
 */
final class FIDATAGroovyProjectPlugins {
  static final Map<String, Map> GRADLE_PLUGINS = [
    'groovy': [:],
    'maven': [:],
    'signing': [:],
  ]

  static final Map<String, Map> THIRDPARTY_PLUGINS = [
    'com.jfrog.bintray': [
      group: 'com.jfrog.bintray.gradle',
      name: 'gradle-bintray-plugin',
      excludes: [
        [
          group: 'xerces',
          module: 'xercesImpl'
        ],
      ]
    ],
  ]

  static final List PLUGIN_IDS = GRADLE_PLUGINS.findAll({ it.value.get('enabled', true) })*.key + THIRDPARTY_PLUGINS.findAll({ it.value.get('enabled', true) })*.key

  // Suppress default constructor for noninstantiability
  private FIDATAGroovyProjectPlugins() {
    throw new AssertionError()
  }
}
