#!/usr/bin/env groovy
/*
 * org.fidata.base.groovy Gradle plugin
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

import static java.nio.charset.StandardCharsets.UTF_8
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.fidata.gradle.utils.PluginDependeesUtils
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc

/**
 * Provides tools for Groovy language
 */
@CompileStatic
final class GroovyBasePlugin extends AbstractProjectPlugin {
  @Override
  protected void doApply() {
    project.pluginManager.apply JvmBasePlugin

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, GroovyBaseProjectPluginDependees.PLUGIN_DEPENDEES

    project.tasks.withType(GroovyCompile).configureEach { GroovyCompile groovyCompile ->
      groovyCompile.options.encoding = UTF_8.name()
    }

    if (!isBuildSrc) {
      configureDocumentation()

      configureArtifacts()
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
    project.extensions.configure(JvmBaseExtension) { JvmBaseExtension extension ->
      extension.javadocLinks['groovy'] = groovydocLink
      extension.javadocLinks['org.codehaus.groovy'] = groovydocLink
    }

    project.tasks.withType(Groovydoc).configureEach { Groovydoc groovydoc ->
      groovydoc.doFirst {
        groovydoc.project.extensions.getByType(JvmBaseExtension).javadocLinks.each { String key, URI value ->
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

  /**
   * Name of groovydocJar task
   */
  public static final String GROOVYDOC_JAR_TASK_NAME = 'groovydocJar'

  /**
   * Classifier of javadoc jar artifact
   */
  public static final String GROOVYDOC_JAR_ARTIFACT_CLASSIFIER = 'groovydoc'

  private TaskProvider<Jar> defaultGroovydocJarProvider

  @PackageScope
  TaskProvider<Jar> getDefaultGroovydocJarProvider() {
    this.@defaultGroovydocJarProvider
  }

  @PackageScope
  private Property<Jar> groovydocJar

  @PackageScope
  Property<Jar> getGroovydocJar() {
    this.@groovydocJar
  }

  private void configureArtifacts() {
    this.@defaultGroovydocJarProvider = project.tasks.register(GROOVYDOC_JAR_TASK_NAME, Jar) { Jar defaultGroovydocJar ->
      defaultGroovydocJar.archiveClassifier.set GROOVYDOC_JAR_ARTIFACT_CLASSIFIER
    }
    this.@groovydocJar = project.objects.property(Jar).convention(defaultGroovydocJarProvider)

    project.afterEvaluate {
      defaultGroovydocJarProvider.configure { Jar defaultGroovydocJar ->
        defaultGroovydocJar.enabled = groovydocJar.get() == defaultGroovydocJar
        null
      }

      project.plugins.getPlugin(JvmBasePlugin).mainPublication.artifact groovydocJar.get()
    }
  }
}
