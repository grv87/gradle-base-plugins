/*
 * org.fidata.dependencies Gradle Plugin
 * Copyright © 2018  Basil Peace
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
package org.fidata.gradle.dependencies

import static org.fidata.gradle.utils.VersionUtils.isPreReleaseVersion
import static org.gradle.internal.component.model.ComponentResolveMetadata.DEFAULT_STATUS_SCHEME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.internal.plugins.DslObject

/**
 * Polishing dependency resolution in Gradle
 */
class DependenciesPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.dependencies.components.all { ComponentMetadataDetails metadata ->
      metadata.with {
        if (status == 'release' && isPreReleaseVersion(id.version)) {
          status = 'milestone'
        }
      }
    }

    project.configurations.configureEach { Configuration configuration ->
      configuration.dependencies.configureEach { Dependency dependency ->
        if (ExternalModuleDependency.isInstance(dependency)) {
          new DslObject(dependency).convention.plugins.put 'dependency', new ExternalModuleDependencyConvention()
        }
      }

      configuration.resolutionStrategy { ResolutionStrategy resolutionStrategy ->
        resolutionStrategy.cacheChangingModulesFor 0, 'seconds'

        componentSelection.all { ComponentSelection selection ->
          if (isPreReleaseVersion(selection.candidate.version)) {
            Integer i = configuration.allDependencies.findAll { Dependency dependency ->
              ExternalModuleDependency.isInstance(dependency) &&
                dependency.group == selection.candidate.group &&
                dependency.name == selection.candidate.module
            }.collect { Dependency dependency ->
              DEFAULT_STATUS_SCHEME.indexOf(new DslObject(dependency).convention.getPlugin(ExternalModuleDependencyConvention).status)
            }.findAll { int i -> i > -1 }.min()
            if (i > DEFAULT_STATUS_SCHEME.indexOf('milestone')) {
              selection.reject 'Pre-release version'
            }
          }
        }
      }
    }
  }
}
