#!/usr/bin/env groovy
/*
 * org.fidata.project.groovy Gradle plugin
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

import static org.gradle.api.plugins.JavaPlugin.API_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME
import org.gradle.api.artifacts.ConfigurationVariant
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.fidata.gradle.utils.PluginDependeesUtils
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.compile.GroovyCompile
import org.ajoberstar.gradle.git.publish.GitPublishExtension
import org.gradle.api.tasks.javadoc.Javadoc

/**
 * Provides an environment for a Groovy project
 */
@CompileStatic
final class GroovyProjectPlugin extends AbstractProjectPlugin {
  @Override
  protected void doApply() {
    project.pluginManager.apply GroovyBasePlugin

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, GroovyProjectPluginDependees.PLUGIN_DEPENDEES

    project.plugins.getPlugin(GroovyBasePlugin).addGroovyDependency project.configurations.named(API_CONFIGURATION_NAME)

    /*
     * CAVEAT:
     * Compatibility with `java-library` plugin. See
     * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues_compat
     * <>
     */
    project.configurations.named(API_ELEMENTS_CONFIGURATION_NAME).configure { Configuration configuration ->
      configuration.outgoing.variants.named('classes').configure { ConfigurationVariant configurationVariant ->
        GroovyCompile compileGroovy = project.tasks.withType(GroovyCompile)['compileGroovy']
        configurationVariant.artifact(
          file: compileGroovy.destinationDir, // TODO
          type: ArtifactTypeDefinition.JVM_CLASS_DIRECTORY,
          builtBy: compileGroovy
        )
      }
    }

    if (!isBuildSrc) {
      configureDocumentation()
    }
  }

  private void configureDocumentation() {
    TaskProvider<Javadoc> javadocProvider = project.tasks.withType(Javadoc).named(JAVADOC_TASK_NAME)
    javadocProvider.configure { Javadoc javadoc ->
      javadoc.enabled = false
    }
    TaskProvider groovydocProvider = project.tasks.withType(Groovydoc).named(GROOVYDOC_TASK_NAME)
    groovydocProvider.configure { Groovydoc groovydoc ->
      groovydoc.source javadocProvider.get().source
    }

    project.rootProject.extensions.getByType(GitPublishExtension).contents.from(project.tasks.named('groovydoc')).into "$project.version/groovydoc"
  }
}
