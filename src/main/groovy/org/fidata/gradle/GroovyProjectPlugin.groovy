#!/usr/bin/env groovy
/*
 * org.fidata.project.groovy Gradle plugin
 * Copyright © 2017  Basil Peace
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

import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.compile.GroovyCompile
import org.ajoberstar.gradle.git.publish.GitPublishExtension
import org.gradle.api.Action
import org.gradle.api.file.CopySpec

/**
 * Provides an environment for a Groovy project
 */
@CompileStatic
public final class GroovyProjectPlugin extends AbstractPlugin {
  public static final String GROOVY_VERSION = GroovySystem.version

  /**
   * Name of Spock reports directory
   */
  public static final String SPOCK_REPORTS_DIR_NAME = 'spock'

  /**
   * List of CodeNarc rules disabled for Spock test sources
   */
  public static final List<String> SPOCK_DISABLED_CODENARC_RULES = ['MethodName', 'FactoryMethodName']

  @Override
  void apply(Project project) {
    super.apply(project)
    project.with {
      plugins.with {
        apply JDKProjectPlugin

        GroovyProjectPluginDependencies.PLUGIN_DEPENDENCIES.findAll() { Map.Entry<String, ? extends Map> depNotation -> depNotation.value.getOrDefault('enabled', true) }.keySet().each { String id ->
          apply id
        }
      }

      dependencies.add('api', [
        group: 'org.codehaus.groovy',
        name: 'groovy-all',
        version: GROOVY_VERSION
      ])
      /*
       * CAVEAT:
       * Compatibility with `java-library` plugin. See
       * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues_compat
       * <>
       */
      configurations.getByName('apiElements').outgoing.variants.getByName('classes').artifact(
        file: tasks.withType(GroovyCompile).getByName('compileGroovy').destinationDir,
        type: ArtifactTypeDefinition.JVM_CLASS_DIRECTORY,
        builtBy: tasks.withType(GroovyCompile).getByName('compileGroovy')
      )

      dependencies.with {
        add('testImplementation', [
          group: 'org.spockframework',
          name: 'spock-core',
          version: "1.1-groovy-${ (GROOVY_VERSION =~ /^(\d+\.\d+)/)[0] }",
        ]) { ModuleDependency dependency ->
          dependency.exclude(
            group: 'org.codehaus.groovy',
            module: 'groovy-all'
          )
        }
        add('testRuntimeOnly', [
          group: 'com.athaydes',
          name: 'spock-reports',
          version: 'latest.release'
        ]) { ModuleDependency dependency ->
          dependency.transitive = false
        }
        add('testImplementation', [
          group: 'org.slf4j',
          name: 'slf4j-api',
          version: 'latest.release'
        ])
        add('testImplementation', [
          group: 'org.slf4j',
          name: 'slf4j-simple',
          version: 'latest.release'
        ])
      }

      new DslObject(convention.getByType(JavaPluginConvention).sourceSets[JDKProjectPlugin.FUNCTIONAL_TEST_SOURCE_SET_NAME]).getConvention().getPlugin(GroovySourceSet).groovy
        /*groovy*/.srcDir file("src/${ JDKProjectPlugin.FUNCTIONAL_TEST_SRC_DIR_NAME }/groovy")


      tasks.withType(Test).getByName(JDKProjectPlugin.FUNCTIONAL_TEST_TASK_NAME).with { Test task ->
        task.with {
          reports.html.enabled = false
          systemProperty 'com.athaydes.spockframework.report.outputDir', new File(convention.getPlugin(ProjectConvention).htmlReportsDir, "$SPOCK_REPORTS_DIR_NAME/${ JDKProjectPlugin.FUNCTIONAL_TEST_REPORTS_DIR_NAME }").absolutePath
        }
      }

      tasks.getByName("codenarc${ JDKProjectPlugin.FUNCTIONAL_TEST_SOURCE_SET_NAME.capitalize() }").setProperty('disabledRules', SPOCK_DISABLED_CODENARC_RULES)

      tasks.withType(Groovydoc) { Groovydoc task ->
        task.with {
          link "https://docs.oracle.com/javase/${ (JavaVersion.toVersion(project.extensions.getByType(JDKExtension).targetVersion) ?:  JavaVersion.current()).majorVersion }/docs/api/", 'java.'
        }
      }

      extensions.getByType(GitPublishExtension).getContents().from(tasks.getByName('groovydoc'), new Action<CopySpec>() {
        public void execute(CopySpec spec) {
          spec.into "$project.version/groovydoc"
        }
      })
    }
  }
}
