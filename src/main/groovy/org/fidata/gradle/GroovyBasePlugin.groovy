#!/usr/bin/env groovy
/*
 * org.fidata.base.groovy Gradle plugin
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

import static java.nio.charset.StandardCharsets.UTF_8
import org.gradle.api.NamedDomainObjectProvider
import org.fidata.gradle.utils.PluginDependeesUtils
import org.gradle.api.artifacts.Configuration
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc

/**
 * Provides tools for Groovy language
 */
@CompileStatic
final class GroovyBasePlugin extends AbstractProjectPlugin {
  @Override
  void apply(Project project) {
    super.apply(project)

    project.pluginManager.apply JVMBasePlugin

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, GroovyBaseProjectPluginDependees.PLUGIN_DEPENDEES

    project.tasks.withType(GroovyCompile).configureEach { GroovyCompile groovyCompile ->
      groovyCompile.options.encoding = UTF_8.name()
    }

    if (!isBuildSrc) {
      configureDocumentation()
    }
  }

  /**
   * Adds groovy to specific configuration
   * @param configuration configuration
   */
  void addGroovyDependency(NamedDomainObjectProvider<Configuration> configuration) {
    project.dependencies.with {
      add configuration.name, localGroovy()
    }
  }

  void configureDocumentation() {
    URI groovydocLink = project.uri("http://docs.groovy-lang.org/${ GroovySystem.version }/html/api/index.html?")
    project.extensions.configure(JVMBaseExtension) { JVMBaseExtension extension ->
      extension.javadocLinks['groovy'] = groovydocLink
      extension.javadocLinks['org.codehaus.groovy'] = groovydocLink
    }

    project.tasks.withType(Groovydoc).configureEach { Groovydoc groovydoc ->
      groovydoc.doFirst {
        groovydoc.project.extensions.getByType(JVMBaseExtension).javadocLinks.each { String key, URI value ->
          groovydoc.link value.toString(), "$key."
        }
      }
      if (!project.rootProject.convention.getPlugin(RootProjectConvention).isRelease.get()) {
        groovydoc.with {
          noTimestamp = true
          noVersionStamp = true
        }
      }
    }
  }
}
