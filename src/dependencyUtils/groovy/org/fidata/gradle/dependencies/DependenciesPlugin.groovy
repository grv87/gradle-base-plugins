/*
 * org.fidata.dependencies Gradle Plugin
 * Copyright © 2018  Basil Peace
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
package org.fidata.gradle.dependencies

import static org.fidata.utils.VersionUtils.isPreReleaseVersion
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
