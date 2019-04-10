#!/usr/bin/env groovy
/*
 * ProjectPluginDependees class
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
import org.fidata.gradle.utils.PluginDependeeExclusion

/**
 * List of dependees of org.fidata.project plugin
 */
@Internal
@CompileStatic
final class ProjectPluginDependees {
  /**
   * List of plugin dependees with IDs
   */
  static final Map<String, PluginDependee> PLUGIN_DEPENDEES = ImmutableMap.copyOf([
    'org.gradle.idea': new PluginDependee(),
    'org.gradle.lifecycle-base': new PluginDependee(),
    'org.fidata.prerequisites': new PluginDependee(
      version: '[1.1.2, 2[',
    ),
    'nebula.contacts': new PluginDependee(
      version: '[4, 6[',
      enabledForBuildSrc: false,
    ),
    'com.github.ben-manes.versions': new PluginDependee(
      configurationName: 'implementation',
      version: '[0, 1[',
    ),
    'com.jfrog.artifactory': new PluginDependee(
      configurationName: 'implementation',
      version: '[4, 5[',
      excludes: [
        new PluginDependeeExclusion(
          group: 'org.codehaus.groovy',
          module: 'groovy-all'
        ),
      ].toSet(),
      enabledForBuildSrc: false,
    ),
    'org.gradle.signing': new PluginDependee(
      enabledForBuildSrc: false,
    ),
    /*
     * WORKAROUND:
     * We use custom fork of semantic-release plugin due to
     * https://github.com/tschulte/gradle-semantic-release-plugin/issues/30 and
     * https://github.com/tschulte/gradle-semantic-release-plugin/issues/31
     * <grv87 2018-06-26>
     */
    'de.gliderpilot.semantic-release': new PluginDependee(
      configurationName: 'implementation',
      group: 'org.fidata.gradle.semantic-release',
      module: 'gradle-semantic-release-plugin',
      version: '[2, 3[',
      enabledForBuildSrc: false,
      enabledForSubprojects: false,
    ),
    'org.ajoberstar.git-publish': new PluginDependee(
      configurationName: 'implementation',
      version: '[1, 2[',
      enabledForBuildSrc: false,
      enabledForSubprojects: false,
    ),
    'org.gradle.reporting-base': new PluginDependee(),
    'org.gradle.codenarc': new PluginDependee(),
    'org.gradle.project-report': new PluginDependee(),
    'com.dorongold.task-tree': new PluginDependee(
      configurationName: 'implementation',
      version: '[1, 2[',
    ),
    /*
     * WORKAROUND:
     * cz.malohlava plugin doesn't work with Gradle 5
     * https://github.com/mmalohlava/gradle-visteg/issues/12
     * <grv87 2018-12-01>
     */
    /*'cz.malohlava': new PluginDependee(
      configurationName: 'implementation',
      version: '[1, 2[',
    ),*/
  ])

  // Suppress default constructor for noninstantiability
  private ProjectPluginDependees() {
    throw new UnsupportedOperationException()
  }
}
