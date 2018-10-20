#!/usr/bin/env groovy
/*
 * JVMBasePluginDependees class
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
import org.fidata.gradle.utils.PluginDependeeExclusion

/**
 * List of dependees of org.fidata.base.jvm plugin
 */
@CompileStatic
final class JVMBasePluginDependees {
  /**
   * List of plugin dependees with IDs
   */
  static final Map<String, PluginDependee> PLUGIN_DEPENDEES = [
    'org.gradle.java-base': new PluginDependee(),
    'org.gradle.java-library': new PluginDependee(),
    'org.gradle.maven-publish': new PluginDependee(
      enabledForBuildSrc: false,
    ),
    'com.jfrog.bintray': new PluginDependee(
      configurationName: 'implementation',
      version: '[1, 2[',
      excludes: [
        /*
         * WORKAROUND:
         * org.apache.maven:maven-ant-tasks has old plexus dependency which have undesired JUnit dependency
         * <grv87 2018-06-24>
         */
        new PluginDependeeExclusion(
          group: 'org.codehaus.plexus',
          module: 'plexus-container-default'
        )
      ],
      enabled: false,
    ),
  ]

  // Suppress default constructor for noninstantiability
  private JVMBasePluginDependees() {
    throw new UnsupportedOperationException()
  }
}
