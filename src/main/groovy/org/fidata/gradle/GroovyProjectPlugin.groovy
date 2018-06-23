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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle

import org.gradle.api.tasks.javadoc.Javadoc

import static GroovyProjectPluginDependencies.PLUGIN_DEPENDENCIES
import static org.gradle.api.plugins.JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.GroovyCompile
import org.ajoberstar.gradle.git.publish.GitPublishExtension

import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME

/**
 * Provides an environment for a Groovy project
 */
@CompileStatic
final class GroovyProjectPlugin extends AbstractPlugin {
  @Override
  void apply(Project project) {
    super.apply(project)
    project.plugins.with {
      apply JDKProjectPlugin

      PLUGIN_DEPENDENCIES.findAll() { Map.Entry<String, ? extends Map> depNotation -> depNotation.value.getOrDefault('enabled', true) }.keySet().each { String id ->
        apply id
      }
    }

    project.dependencies.add('api', [
      group: 'org.codehaus.groovy',
      name: 'groovy-all',
      version: GroovySystem.version
    ])
    /*
     * CAVEAT:
     * Compatibility with `java-library` plugin. See
     * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues_compat
     * <>
     */
    project.configurations.getByName(API_ELEMENTS_CONFIGURATION_NAME).outgoing.variants.getByName('classes').artifact(
      file: project.tasks.withType(GroovyCompile).getByName('compileGroovy').destinationDir,
      type: ArtifactTypeDefinition.JVM_CLASS_DIRECTORY,
      builtBy: project.tasks.withType(GroovyCompile).getByName('compileGroovy')
    )

    configureGroovydoc()

    project.extensions.getByType(GitPublishExtension).contents.from(project.tasks.getByName('groovydoc')).into "$project.version/groovydoc"
  }

  private void configureGroovydoc() {
    Javadoc javadoc = project.tasks.withType(Javadoc).getByName(JAVADOC_TASK_NAME)
    javadoc.onlyIf{ false }
    project.tasks.withType(Groovydoc) { Groovydoc task ->
      task.with {
        source javadoc.source
        project.plugins.getPlugin(JDKProjectPlugin).getJavadocLinks().each { String key, GString value ->
          link value, "$key."
        }
        GString groovydocLink = "http://docs.groovy-lang.org/${ GroovySystem.version }/html/api/"
        link groovydocLink, "groovy."
        link groovydocLink, "org.codehaus.groovy."
      }
    }
  }
}
