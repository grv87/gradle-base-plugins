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
      group: 'org.fidata.gradle',
      module: 'gradle-prerequisites-plugin',
    ),
    'nebula.contacts': new PluginDependee(
      group: 'com.netflix.nebula',
      module: 'gradle-contacts-plugin',
    ),
    'com.github.ben-manes.versions': new PluginDependee(
      configurationName: 'implementation',
      group: 'com.github.ben-manes',
      module: 'gradle-versions-plugin',
    ),
    'com.jfrog.artifactory': new PluginDependee(
      configurationName: 'implementation',
      group: 'org.jfrog.buildinfo',
      module: 'build-info-extractor-gradle',
      excludes: [
        new PluginDependeeExclusion(
          group: 'org.codehaus.groovy',
          module: 'groovy-all'
        ),
      ]
    ),
    'org.gradle.signing': new PluginDependee(),
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
      version: '[2,3)'
    ),
    'org.ajoberstar.git-publish': new PluginDependee(
      configurationName: 'implementation',
      group: 'org.ajoberstar',
      module: 'gradle-git-publish',
    ),
    'org.gradle.reporting-base': new PluginDependee(),
    'org.gradle.codenarc': new PluginDependee(),
    'org.gradle.project-report': new PluginDependee(),
    'com.dorongold.task-tree': new PluginDependee(
      configurationName: 'implementation',
      group: 'gradle.plugin.com.dorongold.plugins',
      module: 'task-tree',
    ),
    'cz.malohlava': new PluginDependee(
      configurationName: 'implementation',
      group: 'cz.malohlava',
      module: 'visteg',
    ),
  ]

  // Suppress default constructor for noninstantiability
  private ProjectPluginDependees() {
    throw new AssertionError()
  }
}
