#!/usr/bin/env groovy
/*
 * org.fidata.project Gradle plugin
 * Copyright © 2017-2018  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import static ProjectPluginDependencies.PLUGIN_DEPENDENCIES
import static org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_TASK_NAME
// import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static nebula.plugin.dependencylock.DependencyLockTaskConfigurer.UPDATE_GLOBAL_LOCK_TASK_NAME
import static nebula.plugin.dependencylock.DependencyLockTaskConfigurer.UPDATE_LOCK_TASK_NAME
import static org.gradle.api.plugins.ProjectReportsPlugin.PROJECT_REPORT
import static org.gradle.initialization.DefaultSettings.DEFAULT_BUILD_SRC_DIR
import static org.gradle.api.Project.DEFAULT_BUILD_DIR_NAME
import static org.gradle.internal.FileUtils.toSafeFileName
import static org.fidata.gradle.utils.VersionUtils.isPreReleaseVersion
import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic
import org.fidata.gradle.internal.AbstractPlugin
import org.gradle.api.Project
// import org.gradle.initialization.DefaultSettings
// import org.gradle.language.base.plugins.LifecycleBasePlugin
// import nebula.plugin.dependencylock.DependencyLockTaskConfigurer
// import org.ajoberstar.gradle.git.publish.GitPublishPlugin
// import org.gradle.api.plugins.ProjectReportsPlugin
import org.gradle.api.Task
import nebula.plugin.dependencylock.tasks.GenerateLockTask
import nebula.plugin.dependencylock.tasks.UpdateLockTask
import nebula.plugin.dependencylock.tasks.SaveLockTask
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.Test
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension
import org.fidata.gradle.tasks.NoJekyll
import org.fidata.gradle.tasks.ResignGitCommit
import org.gradle.buildinit.tasks.internal.TaskConfiguration
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.plugins.ProjectReportsPlugin
import org.gradle.api.plugins.ProjectReportsPluginConvention
import org.gradle.api.tasks.diagnostics.BuildEnvironmentReportTask
import org.gradle.api.reporting.components.ComponentReport
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import org.gradle.api.tasks.diagnostics.DependencyInsightReportTask
import org.gradle.api.reporting.dependents.DependentComponentsReport
import org.gradle.api.reporting.model.ModelReport
import org.gradle.api.tasks.diagnostics.ProjectReportTask
import org.gradle.api.tasks.diagnostics.PropertyReportTask
import org.gradle.api.reporting.dependencies.HtmlDependencyReportTask
import org.gradle.api.tasks.diagnostics.TaskReportTask
import org.fidata.gradle.tasks.InputsOutputs
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.api.file.FileTreeElement
import groovy.text.StreamingTemplateEngine
import groovy.text.Template
import com.google.common.io.Resources
import com.google.common.base.Charsets
import org.gradle.api.logging.LogLevel
import org.fidata.gradle.ProjectPluginDependencies
import cz.malohlava.VisTaskExecGraphPlugin
import cz.malohlava.VisTegPluginExtension
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.api.reporting.ReportingExtension
import nebula.plugin.dependencylock.DependencyLockExtension
// import org.ajoberstar.gradle.git.publish.GitPublishExtension
import org.ajoberstar.gradle.git.ghpages.GithubPagesPluginExtension
import org.ajoberstar.gradle.git.ghpages.GithubPagesPlugin
import org.gradle.api.tasks.util.PatternFilterable
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.artifacts.ComponentSelectionRules
import org.gradle.api.artifacts.ResolutionStrategy
// TODO https://github.com/tschulte/gradle-semantic-release-plugin/issues/31 import de.gliderpilot.gradle.semanticrelease.UpdateGithubRelease
import org.gradle.api.Action

/**
 * Provides an environment for a general, language-agnostic project
 */
// @CompileStatic
final class ProjectPlugin extends AbstractPlugin {
  static final Template COMMIT_MESSAGE_TEMPLATE = new StreamingTemplateEngine().createTemplate(
    '''
      $type: $subject

      Generated by $generatedBy
    '''.stripIndent()
  )

  /**
   * List of filenames considered as license files
   */
  public static final List<String> LICENSE_FILE_NAMES = [
    // These filenames are recognized by JFrog Artifactory
    'license',
    'LICENSE',
    'license.txt',
    'LICENSE.txt',
    'LICENSE.TXT',
    // These are GPL standard file names
    'COPYING',
    'COPYING.LESSER',
  ]

  @Override
  @SuppressWarnings('CouldBeElvis')
  void apply(Project project) {
    super.apply(project)
    project.with {
      PLUGIN_DEPENDENCIES.findAll() { Map.Entry<String, ? extends Map> depNotation -> depNotation.value.getOrDefault('enabled', true) }.keySet().each { String id ->
        plugins.apply id
      }

      // ProjectConvention projectConvention =
      convention.plugins['fidata'] = new ProjectConvention(project)
      // convention.create('fidata', ProjectConvention, project)
      // convention.add('fidata', ProjectConvention, project)


      tasks.withType(Wrapper) { Wrapper task ->
        task.with {
          group = 'Chore'
          gradleVersion = '4.3.1'
        }
      }

      if (!group) { group = 'org.fidata' }

      extensions.getByType(ReportingExtension).baseDir = convention.getPlugin(ProjectConvention).reportsDir
    }

    configureLifecycle()

    configureBuildToolsLifecycle()

    configureDependencyResolution()

    configureArtifactory()

    configureSigning()

    configureGit()

    configureSemanticRelease()

    configureGitPublish()

    configureCodeNarc()

    configureDiagnostics()

    project.extensions.getByType(EclipseModel).classpath.with {
      downloadSources = true
      downloadJavadoc = true
    }
  }

  /**
   * Name of lint task
   */
  public static final String LINT_TASK_NAME = 'lint'

  private void configureLifecycle() {
    project.with {
      tasks.getByName(BUILD_TASK_NAME).dependsOn.remove CHECK_TASK_NAME
      tasks.getByName(/*RELEASE_TASK_NAME*/ 'release').with {
        // dependsOn BUILD_TASK_NAME TOTEST
        dependsOn(tasks.getByName(CHECK_TASK_NAME))
        if (project.convention.getPlugin(ProjectConvention).isRelease) {
          dependsOn(tasks.getByName(/*GitPublishPlugin.PUSH_TASK*/ 'gitPublishPush'))
        }
      }
      Task lintTask = task(LINT_TASK_NAME) {
        group = 'Verification'
        description = 'Runs all static code analyses'
      }
      tasks.getByName(CHECK_TASK_NAME).dependsOn lintTask
      tasks.withType(Test) { Test task ->
        task.project.tasks.getByName(CHECK_TASK_NAME).dependsOn task
      }
    }
  }

  /**
   * Name of buildToolsInstall task
   */
  public static final String BUILD_TOOLS_INSTALL_TASK_NAME = 'buildToolsInstall'
  /**
   * Name of buildToolsUpdate task
   */
  public static final String BUILD_TOOLS_UPDATE_TASK_NAME = 'buildToolsUpdate'
  /**
   * Name of buildToolsOutdated task
   */
  public static final String BUILD_TOOLS_OUTDATED_TASK_NAME = 'buildToolsOutdated'

  @SuppressWarnings('BracesForForLoop')
  private void configureBuildToolsLifecycle() {
    project.with {
      Task buildToolsInstall = task(BUILD_TOOLS_INSTALL_TASK_NAME) { Task task ->
        task.with {
          group = TaskConfiguration.GROUP
          description = 'Install all buildTools for build'
        }
      }
      Task buildToolsUpdate = task(BUILD_TOOLS_UPDATE_TASK_NAME) { Task task ->
        task.with {
          group = TaskConfiguration.GROUP
          description = 'Update all buildTools that support automatic update'
          mustRunAfter buildToolsInstall
        }
      }
      Task buildToolsOutdated = task(BUILD_TOOLS_OUTDATED_TASK_NAME) { Task task ->
        task.with {
          group = TaskConfiguration.GROUP
          description = 'Show outdated buildTools'
          mustRunAfter buildToolsInstall
        }
      }
      afterEvaluate {
        for (Task task in
          tasks
          - buildToolsInstall
          - buildToolsInstall.taskDependencies.getDependencies(buildToolsInstall)
          - buildToolsInstall.mustRunAfter.getDependencies(buildToolsInstall)
          - buildToolsInstall.shouldRunAfter.getDependencies(buildToolsInstall)
        ) {
          task.mustRunAfter buildToolsInstall
        }
        for (Task task in
            tasks
            - buildToolsUpdate
            - buildToolsUpdate.taskDependencies.getDependencies(buildToolsUpdate)
            - buildToolsUpdate.mustRunAfter.getDependencies(buildToolsUpdate)
            - buildToolsUpdate.shouldRunAfter.getDependencies(buildToolsUpdate)
        ) {
          task.mustRunAfter buildToolsUpdate
        }
      }

      tasks.withType(GenerateLockTask) { GenerateLockTask task ->
        task.group = null
        buildToolsInstall.mustRunAfter task
      }
      tasks.withType(UpdateLockTask) { UpdateLockTask task ->
        task.group = 'Chore'
        buildToolsUpdate.mustRunAfter task
      }
      tasks.withType(SaveLockTask) { SaveLockTask task ->
        task.group = 'Chore'
        buildToolsUpdate.mustRunAfter task
      }
      tasks.findByName('saveGlobalLock')?.with {
        dependsOn tasks.getByName(UPDATE_GLOBAL_LOCK_TASK_NAME)
      }
      tasks.getByName('saveLock').with {
        dependsOn tasks.getByName(UPDATE_LOCK_TASK_NAME)
      }
      buildToolsUpdate.with {
        if (tasks.withType(SaveLockTask).findByName('saveGlobalLock')?.outputLock?.exists()) {
          dependsOn tasks.getByName('saveGlobalLock')
        }
        else {
          dependsOn tasks.getByName('saveLock')
        }
      }

      tasks.withType(DependencyUpdatesTask) { DependencyUpdatesTask task ->
        task.group = 'Chore'
        buildToolsOutdated.dependsOn task
      }
    }
  }

  private void configureDependencyResolution() {
    project.with {
      extensions.getByType(DependencyLockExtension).includeTransitives = true

      dependencies.components.all { ComponentMetadataDetails details ->
        if (details.status == 'release' && isPreReleaseVersion(details.id.version)) {
          details.status = 'milestone'
        }
      }

      tasks.withType(DependencyUpdatesTask) { DependencyUpdatesTask task ->
        task.with {
          revision = 'release'
          outputFormatter = 'xml'
          outputDir = new File(project.convention.getPlugin(ProjectConvention).xmlReportsDir, 'dependencyUpdates')
          resolutionStrategy = { ResolutionStrategy resolutionStrategy ->
            resolutionStrategy.componentSelection { ComponentSelectionRules rules ->
              rules.all { ComponentSelection selection ->
                if (revision == 'release' && isPreReleaseVersion(selection.candidate.version)) {
                  selection.reject 'Pre-release version'
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * URL of FIDATA Artifactory
   */
  public static final String ARTIFACTORY_URL = 'https://fidata.jfrog.io/fidata'

  /*
   * CAVEAT:
   * Conventions and extensions in JFrog Gradle plugins have package scopes,
   * so we can't use static compilation
   * <grv87 2018-02-18>
   */
  @CompileDynamic
  private void configureArtifactory() {
    project.with {
      if (project.hasProperty('artifactoryUser') && project.hasProperty('artifactoryPassword')) {
        artifactory {
          contextUrl = ARTIFACTORY_URL
          resolve {
            repository {
              repoKey = project.convention.getPlugin(ProjectConvention).isRelease ? 'libs-release' : 'libs-snapshot'
              username = project.property('artifactoryUser')
              password = project.extensions.extraProperties['artifactoryPassword'] // TOTEST
              maven = true
            }
          }
        }
      }
      else {
        repositories {
          jcenter()
          mavenCentral()
        }
      }
    }
  }

  private void configureSigning() {
    System.setProperty 'signing.keyId', project.property('gpgKeyId').toString()
    System.setProperty 'signing.password', project.property('gpgKeyPassword').toString()
    System.setProperty 'signing.secretKeyRingFile', project.property('gpgSecretKeyRingFile').toString()
  }

  private void configureGit() {
    if (project.hasProperty('gitUsername') && project.property('gitPassword')) {
      System.setProperty 'org.ajoberstar.grgit.auth.username', project.property('gitUsername').toString()
      System.setProperty 'org.ajoberstar.grgit.auth.password', project.property('gitPassword').toString()
    }
  }

  @CompileDynamic
  private void configureSemanticRelease() {
    if (project.hasProperty('ghToken')) {
      /*project.extensions.getByType(SemanticReleasePluginExtension.class)*/
      // project.tasks.withType(UpdateGithubRelease).getByName('updateGithubRelease').repo.ghToken = project.property('ghToken').toString()
      project.tasks.getByName('updateGithubRelease').repo.ghToken = project.property('ghToken').toString()
    }
  }

  /**
   * Name of NoJekyll task
   */
  public static final String NO_JEKYLL_TASK_NAME = 'noJekyll'

  private void configureGitPublish() {
    project.with {
      extensions.getByType(/*GitPublishExtension*/ GithubPagesPluginExtension).with {
        targetBranch = 'gh-pages'
        deleteExistingFiles = false
        /*preserve { PatternFilterable preserve ->
          preserve.with {
            include '**'
            exclude '*-SNAPSHOT/**' // TODO - keep other branches ?
          }
        }*/
        commitMessage = COMMIT_MESSAGE_TEMPLATE.make(
          type: 'docs',
          subject: "publish documentation for version $version",
          generatedBy: 'org.ajoberstar:gradle-git-publish'
        )
      }

      NoJekyll noJekyllTask = tasks.create(NO_JEKYLL_TASK_NAME, NoJekyll)
      noJekyllTask.with {
        description = 'Generates .nojekyll file in gitPublish repository'
        // destinationDir = project.extensions.getByType(GitPublishExtension).repoDir
        destinationDir = project.file(project.extensions.getByType(GithubPagesPluginExtension).workingPath)
      }
      tasks.getByName(/*GitPublishPlugin.COMMIT_TASK*/ GithubPagesPlugin.PUBLISH_TASK_NAME).dependsOn noJekyllTask

      /*
       * BLOCKED:
       * JGit doesn't support signed commits yet.
       * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=382212 <>
       */
      ResignGitCommit resignGitCommit = tasks.create("${ /*GitPublishPlugin.COMMIT_TASK*/ GithubPagesPlugin.PUBLISH_TASK_NAME }Resign", ResignGitCommit)
      resignGitCommit.description = 'Amend git publish commit adding sign to it'
      tasks.getByName(/*GitPublishPlugin.COMMIT_TASK*/ GithubPagesPlugin.PUBLISH_TASK_NAME).finalizedBy resignGitCommit
    }
  }

  /**
   * Name of CodeNarc common task
   */
  public static final String CODENARC_TASK_NAME = 'codenarc'

  private static final String CODENARC_DEFAULT_CONFIG = Resources.toString(Resources.getResource(ProjectPlugin, 'config/codenarc/codenarc.groovy'), Charsets.UTF_8)

  private static final Template CODENARC_DISABLED_RULES_CONFIG_TEMPLATE = new StreamingTemplateEngine().createTemplate(Resources.toString(Resources.getResource(ProjectPlugin, 'config/codenarc/codenarc.disabledRules.groovy.template'), Charsets.UTF_8))

  private void configureCodeNarc() {
    project.with {
      Task codeNarcTask = task(CODENARC_TASK_NAME) { Task task ->
        task.with {
          group = 'Verification'
          description = 'Runs CodeNarc analysis for each source set'
        }
      }
      tasks.getByName(LINT_TASK_NAME).dependsOn codeNarcTask

      tasks.withType(CodeNarc) { CodeNarc task ->
        task.with {
          config = project.resources.text.fromString(CODENARC_DEFAULT_CONFIG)
          doFirst {
            if (task.extensions.extraProperties.has('disabledRules')) {
              config = project.resources.text.fromString(
                config.asString() +
                CODENARC_DISABLED_RULES_CONFIG_TEMPLATE.make(disabledRules: task.extensions.extraProperties['disabledRules'].inspect())
              )
            }
          }
          String reportFileName = "codenarc/${ toSafeFileName((name - ~/^codenarc/).uncapitalize()) }"
          reports.with {
            xml.with {
              enabled = true
              setDestination new File(project.convention.getPlugin(ProjectConvention).xmlReportsDir, "${ reportFileName }.xml")
            }
            // console.enabled = true TODO
            html.with {
              enabled = true
              setDestination new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, "${ reportFileName }.html")
            }
          }
        }
        codeNarcTask.dependsOn task
      }

      task("codenarc${ DEFAULT_BUILD_SRC_DIR.capitalize() }", type: CodeNarc) { CodeNarc task ->
        Closure excludeBuildDir = { FileTreeElement fte ->
          String[] p = fte.relativePath.segments
          int i = 0
          while (i < p.length && p[i] == DEFAULT_BUILD_SRC_DIR) { i++ }
          i < p.length && p[i] == DEFAULT_BUILD_DIR_NAME
        }
        task.with {
          for (File f in fileTree(projectDir) {
            include '**/*.gradle'
            exclude excludeBuildDir
          }) {
            source f
          }
          for (File f in fileTree(DEFAULT_BUILD_SRC_DIR) {
            include '**/*.groovy'
            exclude excludeBuildDir
          }) {
            source f
          }
          source 'Jenkinsfile'
          source fileTree(dir: file('config'), includes: ['**/*.groovy'])
        }
      }
    }
  }

  /**
   * Name of InputsOutputs task
   */
  public static final String INPUTS_OUTPUTS_TASK_NAME = 'inputsOutputs'

  private void configureDiagnostics() {
    project.with {
      plugins.withType(ProjectReportsPlugin) {
        convention.getPlugin(ProjectReportsPluginConvention).projectReportDirName = convention.getPlugin(ProjectConvention).reportsDir.toPath().relativize(new File(convention.getPlugin(ProjectConvention).txtReportsDir, 'project').toPath()).toString()

        tasks.withType(BuildEnvironmentReportTask) { BuildEnvironmentReportTask task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(ComponentReport) { ComponentReport task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(DependencyReportTask) { DependencyReportTask task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(DependencyInsightReportTask) { DependencyInsightReportTask task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(DependentComponentsReport) { DependentComponentsReport task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(ModelReport) { ModelReport task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(ProjectReportTask) { ProjectReportTask task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(PropertyReportTask) { PropertyReportTask task ->
          task.group = 'Diagnostics'
        }
        tasks.withType(HtmlDependencyReportTask) { HtmlDependencyReportTask task ->
          task.with {
            group = 'Diagnostics'
            reports.html.setDestination new File(project.convention.getPlugin(ProjectConvention).htmlReportsDir, 'dependencies')
          }
        }
        tasks.withType(TaskReportTask) { TaskReportTask task ->
          task.group = 'Diagnostics'
        }
        tasks.getByName(PROJECT_REPORT).group = 'Diagnostics'
      }

      task(INPUTS_OUTPUTS_TASK_NAME, type: InputsOutputs) { InputsOutputs task ->
        task.with {
          group = 'Diagnostics'
          description = 'Generates report about all task file inputs and outputs'
          outputFile = new File(project.convention.getPlugin(ProjectConvention).txtReportsDir, InputsOutputs.DEFAULT_OUTPUT_FILE_NAME)
        }
      }

      plugins.withType(VisTaskExecGraphPlugin) {
        extensions.getByType(VisTegPluginExtension).with {
          enabled        = (logging.level ?: gradle.startParameter.logLevel) <= LogLevel.INFO
          colouredNodes  = true
          colouredEdges  = true
          destination    = new File(project.convention.getPlugin(ProjectConvention).reportsDir, 'visteg.dot')
          exporter       = 'dot'
          colorscheme    = 'paired12'
          nodeShape      = 'box'
          startNodeShape = 'hexagon'
          endNodeShape   = 'doubleoctagon'
        }
      }
    }
  }
}
