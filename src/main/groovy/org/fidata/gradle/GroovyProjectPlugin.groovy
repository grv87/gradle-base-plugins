#!/usr/bin/env groovy
/*
 * org.fidata.project.groovy Gradle plugin
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

import static org.gradle.api.plugins.JavaPlugin.API_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import static org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME
import org.gradle.api.tasks.TaskProvider
import org.fidata.gradle.utils.PluginDependeesUtils
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.gradle.api.Project
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
  void apply(Project project) {
    super.apply(project)

    project.pluginManager.apply GroovyBasePlugin

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, GroovyProjectPluginDependees.PLUGIN_DEPENDEES

    project.plugins.getPlugin(GroovyBasePlugin).addGroovyDependency project.configurations.getByName(API_CONFIGURATION_NAME)

    /*
     * CAVEAT:
     * Compatibility with `java-library` plugin. See
     * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues_compat
     * <>
     */
    project.configurations.getByName(API_ELEMENTS_CONFIGURATION_NAME).outgoing.variants.getByName('classes').artifact(
      file: project.tasks.withType(GroovyCompile).getByName('compileGroovy').destinationDir, // TODO
      type: ArtifactTypeDefinition.JVM_CLASS_DIRECTORY,
      builtBy: project.tasks.withType(GroovyCompile).getByName('compileGroovy')
    )

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
