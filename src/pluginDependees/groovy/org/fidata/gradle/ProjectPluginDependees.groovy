#!/usr/bin/env groovy
/*
 * ProjectPluginDependees class
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
 * List of dependees of org.fidata.project plugin
 */
@CompileStatic
final class ProjectPluginDependees {
  /**
   * List of plugin dependees with IDs
   */
  static final Map<String, PluginDependee> PLUGIN_DEPENDEES = [
    'org.gradle.idea': new PluginDependee(),
    'org.gradle.lifecycle-base': new PluginDependee(),
    'org.fidata.prerequisites': new PluginDependee(
      version: '[1.1.2, 2[',
    ),
    'nebula.contacts': new PluginDependee(
      version: '[4, 5[',
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
      ],
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
    'cz.malohlava': new PluginDependee(
      configurationName: 'implementation',
      version: '[1, 2[',
    ),
  ]

  // Suppress default constructor for noninstantiability
  private ProjectPluginDependees() {
    throw new UnsupportedOperationException()
  }
}
