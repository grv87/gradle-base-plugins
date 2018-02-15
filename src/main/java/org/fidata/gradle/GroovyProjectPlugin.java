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
package org.fidata.gradle;

import org.fidata.gradle.internal.AbstractPlugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.javadoc.Groovydoc;
import org.gradle.api.JavaVersion;
import org.gradle.internal.jvm.Jvm;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;

/**
 * Provides an environment for a Groovy project
 */
public final class GroovyProjectPlugin extends AbstractPlugin {
  public static final GROOVY_VERSION = GroovySystem.version

  /**
   * Name of Spock reports directory
   */
  public static final String SPOCK_REPORTS_DIR_NAME = 'spock'

  /**
   * List of CodeNarc rules disabled for Spock test sources
   */
  public static final List<String> SPOCK_DISABLED_CODENARC_RULES = ['MethodName', 'FactoryMethodName']

  @Override
  public void apply(Project project) {
    super.apply(project)
    project.with {
      apply plugin: JDKProjectPlugin
      for (String id in GroovyProjectPluginDependencies.DEFAULT_PLUGINS) {
        plugins.apply id
      }

      dependencies {
        api(
          group: 'org.codehaus.groovy',
          name: 'groovy-all',
          version: GROOVY_VERSION
        )
      }
      /*
       * CAVEAT:
       * Compatibility with `java-library` plugin. See
       * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues_compat
       * <>
       */
      configurations {
        apiElements {
          outgoing.variants.getByName('classes').artifact(
            file: compileGroovy.destinationDir,
            type: ArtifactTypeDefinition.JVM_CLASS_DIRECTORY,
            builtBy: compileGroovy
          )
        }
      }

      dependencies {
        testImplementation(
          group: 'org.spockframework',
          name: 'spock-core',
          version: "1.1-groovy-${ (GROOVY_VERSION =~ /^(\d+\.\d+)/)[0][0] }",
        ) {
          exclude(
            group: 'org.codehaus.groovy',
            module: 'groovy-all'
          )
        }
        testRuntimeOnly(
          group: 'com.athaydes',
          name: 'spock-reports',
          version: 'latest.release'
        ) {
          transitive = false
        }
        testImplementation(
          group: 'org.slf4j',
          name: 'slf4j-api',
          version: 'latest.release'
        )
        testImplementation(
          group: 'org.slf4j',
          name: 'slf4j-simple',
          version: 'latest.release'
        )
      }

      sourceSets[JDKProjectPlugin.FUNCTIONAL_TEST_SOURCE_SET_NAME].with {
        groovy.srcDir file("src/${ JDKProjectPlugin.FUNCTIONAL_TEST_SRC_DIR_NAME }/groovy")
      }

      tasks[JDKProjectPlugin.FUNCTIONAL_TEST_TASK_NAME].with {
        reports.html.enabled = false
        systemProperty 'com.athaydes.spockframework.report.outputDir', new File(htmlReportsDir, "$SPOCK_REPORTS_DIR_NAME/${ JDKProjectPlugin.FUNCTIONAL_TEST_REPORTS_DIR_NAME }").absolutePath
      }

      tasks["codenarc${ JDKProjectPlugin.FUNCTIONAL_TEST_SOURCE_SET_NAME.capitalize() }"].ext.disabledRules = SPOCK_DISABLED_CODENARC_RULES

      tasks.withType(Groovydoc) { Groovydoc task ->
        task.with {
          link "https://docs.oracle.com/javase/${ (JavaVersion.toVersion(project.jdk.targetVersion) ?:  Jvm.current()).javaVersion.majorVersion }/docs/api/", 'java.'
        }
      }

      gitPublish {
        contents {
          from(groovydoc) {
            into "$version/groovydoc"
          }
        }
      }
    }
  }
}
