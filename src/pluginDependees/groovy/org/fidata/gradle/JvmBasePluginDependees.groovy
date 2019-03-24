#!/usr/bin/env groovy
/*
 * JvmBasePluginDependees class
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
package org.fidata.gradle

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.fidata.gradle.utils.PluginDependee
import org.fidata.gradle.utils.PluginDependeeExclusion

/**
 * List of dependees of org.fidata.base.jvm plugin
 */
@CompileStatic
final class JvmBasePluginDependees {
  /**
   * List of plugin dependees with IDs
   */
  static final Map<String, PluginDependee> PLUGIN_DEPENDEES = ImmutableMap.copyOf([
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
      ].toSet(),
      enabled: false,
    ),
  ])

  // Suppress default constructor for noninstantiability
  private JvmBasePluginDependees() {
    throw new UnsupportedOperationException()
  }
}
