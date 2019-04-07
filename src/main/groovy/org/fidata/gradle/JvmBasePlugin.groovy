#!/usr/bin/env groovy
/*
 * org.fidata.project.jdk Gradle plugin
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

import static ProjectPlugin.LICENSE_FILE_NAMES
import static java.nio.charset.StandardCharsets.UTF_8
import static org.ajoberstar.gradle.git.release.base.BaseReleasePlugin.RELEASE_TASK_NAME
import static org.gradle.api.plugins.JavaPlugin.API_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CONFIGURATION_NAME
import static org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import static org.gradle.initialization.IGradlePropertiesLoader.ENV_PROJECT_PROPERTIES_PREFIX
import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME
import static org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME
import com.google.common.collect.ImmutableSet
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayPublishTask
import de.gliderpilot.gradle.semanticrelease.GitRepo
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension
import groovy.transform.CompileStatic
import groovy.transform.Internal
import groovy.transform.PackageScope
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import org.fidata.gradle.internal.AbstractProjectPlugin
import org.fidata.gradle.tasks.CodeNarcTaskConvention
import org.fidata.gradle.utils.PathDirector
import org.fidata.gradle.utils.PluginDependeesUtils
import org.fidata.gradle.utils.ReportPathDirectorException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Namer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.plugins.quality.FindBugs
import org.gradle.api.plugins.quality.JDepend
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyArtifact
import org.gradle.api.publish.ivy.internal.publication.IvyPublicationInternal
import org.gradle.api.publish.ivy.internal.publisher.IvyNormalizedPublication
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.api.publish.maven.internal.publisher.MavenNormalizedPublication
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

/**
 * Provides an environment for a JDK project
 */
@CompileStatic
final class JvmBasePlugin extends AbstractProjectPlugin implements PropertyChangeListener {
  /**
   * Name of jvm extension for {@link Project}
   */
  public static final String JVM_EXTENSION_NAME = 'jvm'

  @Override
  protected void doApply() {
    if (project == project.rootProject) {
      project.pluginManager.apply ProjectPlugin
    }

    boolean isBuildSrc = project.rootProject.convention.getPlugin(RootProjectConvention).isBuildSrc

    PluginDependeesUtils.applyPlugins project, isBuildSrc, JvmBasePluginDependees.PLUGIN_DEPENDEES

    project.extensions.add JVM_EXTENSION_NAME, new JvmBaseExtension(project)

    project.convention.getPlugin(ProjectConvention).addPropertyChangeListener this

    if (!isBuildSrc) {
      configurePublicReleases()
    }

    project.tasks.withType(JavaCompile).configureEach { JavaCompile javaCompile ->
      javaCompile.options.encoding = UTF_8.name()
    }

    if (!isBuildSrc) {
      project.tasks.withType(ProcessResources).configureEach { ProcessResources processResources ->
        processResources.from(LICENSE_FILE_NAMES) { CopySpec copySpec ->
          copySpec.into 'META-INF'
        }
      }
    }

    if (!isBuildSrc) {
      configureDocumentation()
    }

    configureTesting()

    if (!isBuildSrc) {
      configureArtifactsPublishing()
    }

    configureCodeQuality()
  }

  /**
   * Gets called when a property is changed
   */
  void propertyChange(PropertyChangeEvent e) {
    switch (e.source) {
      case project.convention.getPlugin(ProjectConvention):
        switch (e.propertyName) {
          case 'publicReleases':
            configurePublicReleases()
            break
        }
        break
      default:
        project.logger.warn('org.fidata.base.jvm: unexpected property change source: {}', e.source)
    }
  }

  private void configurePublicReleases() {
    if (project.convention.getPlugin(ProjectConvention).publicReleases) {
      configureMavenCentral()
      configureBintray()
      configureGithubReleases()
    }
  }

  /*
   * WORKAROUND:
   * Static fields annotated with @PackageScope are not accessible
   * for inner classes (incl. closures)
   * https://issues.apache.org/jira/browse/GROOVY-9043
   * <grv87 2019-03-19>
   */
  @Internal
  static final String JUNIT_GROUP = 'junit'

  @Internal
  static final String JUNIT_MODULE = 'junit'

  /**
   * Adds JUnit dependency to specified source set configuration
   * @param sourceSet source set
   */
  void addJUnitDependency(NamedDomainObjectProvider<SourceSet> sourceSetProvider) {
    sourceSetProvider.configure { SourceSet sourceSet ->
      project.dependencies.add(sourceSet.implementationConfigurationName, [
        group: JUNIT_GROUP,
        name: JUNIT_MODULE,
        version: '[4, 5['
      ])
    }
  }

  /**
   * Adds Spock to specified source set and task
   * @param sourceSet source set
   * @param task test task.
   *        If null, task with the same name as source set is used
   */
  void addSpockDependency(NamedDomainObjectProvider<SourceSet> sourceSetProvider, TaskProvider<Test> task = null) {
    addSpockDependency sourceSetProvider, [task ?: project.tasks.withType(Test).named(sourceSetProvider.name)], new PathDirector<TaskProvider<Test>>() {
      @Override
      Path determinePath(TaskProvider<Test> object)  {
        try {
          Paths.get(object.name)
        } catch (InvalidPathException e) {
          throw new ReportPathDirectorException(object, e)
        }
      }
    }
  }

  /**
   * Namer of codenarc task for source sets
   */
  static final Namer<NamedDomainObjectProvider<SourceSet>> CODENARC_NAMER = new Namer<NamedDomainObjectProvider<SourceSet>>() {
    @Override
    String determineName(NamedDomainObjectProvider<SourceSet> sourceSetProvider)  {
      "codenarc${ sourceSetProvider.name.capitalize() }"
    }
  }

  /*
   * WORKAROUND:
   * Static fields annotated with @PackageScope are not accessible
   * for inner classes (incl. closures)
   * https://issues.apache.org/jira/browse/GROOVY-9043
   * <grv87 2019-03-19>
   */
  @Internal
  static final String SPOCK_GROUP = 'org.spockframework'

  @Internal
  static final String SPOCK_MODULE = 'spock-core'

  private static final Set<String> SPOCK_GROOVY_MODULES = ImmutableSet.of(
    'groovy-all',
    'groovy-json',
    'groovy-macro',
    'groovy-nio',
    'groovy-sql',
    'groovy-templates',
    'groovy-test',
    'groovy-xml',
    'groovy',
  )

  /**
   * Adds Spock to specified source set and tasks
   * @param sourceSet source set
   * @param tasks list of test tasks.
   * @param reportDirector path director for task reports
   */
  /*
   * WORKAROUND:
   * Groovy error. Usage of `destination =` instead of setDestination leads to error:
   * [Static type checking] - Cannot set read-only property: destination
   * Also may be CodeNarc error
   * <grv87 2018-06-26>
   */
  @SuppressWarnings('UnnecessarySetter')
  void addSpockDependency(NamedDomainObjectProvider<SourceSet> sourceSetProvider, Iterable<TaskProvider<Test>> tasks, PathDirector<TaskProvider<Test>> reportDirector) {
    addJUnitDependency sourceSetProvider

    project.pluginManager.apply GroovyBasePlugin

    sourceSetProvider.configure { SourceSet sourceSet ->
      project.dependencies.with {
        add(sourceSet.implementationConfigurationName, [
          group: SPOCK_GROUP,
          name: SPOCK_MODULE,
          version: "1.3-groovy-${ (GroovySystem.version =~ /^\d+\.\d+/)[0] }"
        ]) { ModuleDependency dependency ->
          SPOCK_GROOVY_MODULES.each { String spockGroovyModule ->
            dependency.exclude(
              group: 'org.codehaus.groovy',
              module: spockGroovyModule
            )
          }
        }
        add(sourceSet.runtimeOnlyConfigurationName, [
          group: 'com.athaydes',
          name: 'spock-reports',
          version: '[1, 2['
        ]) { ModuleDependency dependency ->
          dependency.transitive = false
        }
      }
      project.plugins.withType(GroovyBasePlugin).configureEach { GroovyBasePlugin plugin ->
        plugin.addGroovyDependency project.configurations.named(sourceSet.implementationConfigurationName)
      }
    }

    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    tasks.each { TaskProvider<Test> taskProvider ->
      taskProvider.configure { Test test ->
        test.with {
          reports.with {
            html.enabled = false
            junitXml.setDestination projectConvention.getXmlReportDir(reportDirector, taskProvider)
          }
          File spockHtmlReportDir = projectConvention.getHtmlReportDir(reportDirector, taskProvider)
          File spockJsonReportDir = projectConvention.getJsonReportDir(reportDirector, taskProvider)
          systemProperty 'com.athaydes.spockframework.report.outputDir', spockHtmlReportDir.absolutePath
          systemProperty 'com.athaydes.spockframework.report.aggregatedJsonReportDir', spockJsonReportDir.absolutePath
          outputs.dir spockHtmlReportDir
          outputs.dir spockJsonReportDir
        }
        /*
         * WORKAROUND:
         * Without that we get error:
         * [Static type checking] - Cannot call org.gradle.api.tasks.TaskProvider <Test>#configure(org.gradle.api.Action
         * <java.lang.Object extends java.lang.Object>) with arguments [groovy.lang.Closure <org.gradle.api.Task>]
         * <grv87 2018-07-31>
         */
        null
      }
    }

    project.plugins.withType(GroovyBasePlugin).configureEach { GroovyBasePlugin plugin -> // TODO: 4.9
      project.tasks.withType(CodeNarc).named(CODENARC_NAMER.determineName(sourceSetProvider)).configure { CodeNarc codenarc ->
        codenarc.convention.getPlugin(CodeNarcTaskConvention).disabledRules.addAll 'MethodName', 'FactoryMethodName', 'JUnitPublicProperty', 'JUnitPublicNonTestMethod'
      }
    }
  }

  /**
   * Configures integration test source set classpath
   * See <a href="https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests">https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests</a>
   * @param sourceSet source set to configure
   */
  void configureIntegrationTestSourceSetClasspath(SourceSet sourceSet) {
    // https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests
    // https://docs.gradle.org/current/userguide/test_kit.html#sub:test-kit-automatic-classpath-injection
    // +
    SourceSet mainSourceSet = project.convention.getPlugin(JavaPluginConvention).sourceSets.getByName(MAIN_SOURCE_SET_NAME)
    sourceSet.compileClasspath += mainSourceSet.output
    sourceSet.runtimeClasspath += sourceSet.output + mainSourceSet.output // TOTEST

    project.configurations.named(sourceSet.implementationConfigurationName).configure { Configuration configuration ->
      configuration.extendsFrom project.configurations.getByName(mainSourceSet.implementationConfigurationName)
    }
    project.configurations.named(sourceSet.runtimeOnlyConfigurationName).configure { Configuration configuration ->
      configuration.extendsFrom project.configurations.getByName(mainSourceSet.runtimeOnlyConfigurationName)
    }
  }

  /**
   * Name of functional test source set
   */
  public static final String FUNCTIONAL_TEST_SOURCE_SET_NAME = 'functionalTest'
  /**
   * Name of functional test source directory
   */
  public static final String FUNCTIONAL_TEST_SRC_DIR_NAME = 'functionalTest'
  /**
   * Name of functional test task
   */
  public static final String FUNCTIONAL_TEST_TASK_NAME = 'functionalTest'

  /*
   * WORKAROUND:
   * Groovy error. Usage of `destination =` instead of setDestination leads to error:
   * [Static type checking] - Cannot set read-only property: destination
   * Also may be CodeNarc error
   * <grv87 2018-06-26>
   */
  @SuppressWarnings('UnnecessarySetter')
  private void configureFunctionalTests() {
    NamedDomainObjectProvider<SourceSet> functionalTestSourceSetProvider = project.convention.getPlugin(JavaPluginConvention).sourceSets.register(FUNCTIONAL_TEST_SOURCE_SET_NAME) { SourceSet sourceSet ->
      sourceSet.java.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/java")
      sourceSet.resources.srcDir project.file("src/$FUNCTIONAL_TEST_SRC_DIR_NAME/resources")

      configureIntegrationTestSourceSetClasspath sourceSet
    }

    TaskProvider<Test> functionalTestProvider = project.tasks.register(FUNCTIONAL_TEST_TASK_NAME, Test) { Test test ->
      SourceSet functionalTestSourceSet = functionalTestSourceSetProvider.get()
      test.with {
        group = VERIFICATION_GROUP
        description = 'Runs functional tests'
        shouldRunAfter project.tasks.named(TEST_TASK_NAME)
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
      }
    }

    addSpockDependency functionalTestSourceSetProvider, functionalTestProvider
  }

  private void configureTesting() {
    project.convention.getPlugin(JavaPluginConvention).with {
      ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
      testReportDirName = project.extensions.getByType(ReportingExtension).baseDir.toPath().relativize(projectConvention.htmlReportsDir.toPath()).toString()
      testResultsDirName = project.buildDir.toPath().relativize(projectConvention.xmlReportsDir.toPath()).toString()
    }
    project.tasks.withType(Test).configureEach { Test test ->
      test.with {
        environment = environment.findAll { String key, Object value -> key != 'GRADLE_OPTS' && !key.startsWith(ENV_PROJECT_PROPERTIES_PREFIX) }
        maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
        testLogging.exceptionFormat = TestExceptionFormat.FULL
      }
    }

    addJUnitDependency project.convention.getPlugin(JavaPluginConvention).sourceSets.named(TEST_SOURCE_SET_NAME)

    configureFunctionalTests()
  }

  /**
   * Namer of sign task for maven publications
   */
  static final Namer<MavenPublication> SIGN_MAVEN_PUBLICATION_NAMER = new Namer<MavenPublication>() {
    @Override
    String determineName(MavenPublication mavenPublication)  {
      "sign${ mavenPublication.name.capitalize() }Publication"
    }
  }

  private void configureArtifactory() {
    project.convention.getPlugin(ArtifactoryPluginConvention).with {
      clientConfig.publisher.repoKey = "libs-${ project.rootProject.convention.getPlugin(RootProjectConvention).isRelease.get() ? 'release' : 'snapshot' }-local"
      clientConfig.publisher.username = project.rootProject.extensions.extraProperties['artifactoryUser'].toString()
      clientConfig.publisher.password = project.rootProject.extensions.extraProperties['artifactoryPassword'].toString()
      clientConfig.publisher.maven = true
    }
    project.tasks.withType(ArtifactoryTask).named(ARTIFACTORY_PUBLISH_TASK_NAME).configure { ArtifactoryTask artifactoryPublish ->
      PublicationContainer publications = project.extensions.getByType(PublishingExtension).publications
      publications.withType(MavenPublication) { MavenPublication mavenPublication ->
        artifactoryPublish.mavenPublications.add mavenPublication
      }
      publications.whenObjectRemoved { MavenPublication mavenPublication ->
        artifactoryPublish.mavenPublications.remove mavenPublication
      }

      artifactoryPublish.dependsOn project.tasks.withType(Sign).matching { Sign sign -> // TODO
        /*
         * WORKAROUND:
         * Without that cast we got compilation error with Groovy 2.5.2:
         * [Static type checking] - Reference to method is ambiguous.
         * Cannot choose between
         * [boolean java.lang.Iterable <T>#any(groovy.lang.Closure),
         * boolean java.lang.Object#any(groovy.lang.Closure)]
         * <grv87 2018-12-01>
         */
        ((Iterable<MavenPublication>)publications.withType(MavenPublication)).any { MavenPublication mavenPublication ->
          sign.name == SIGN_MAVEN_PUBLICATION_NAMER.determineName(mavenPublication)
        }
      }
    }
    project.rootProject.tasks.named(RELEASE_TASK_NAME).configure { Task release ->
      release.finalizedBy project.tasks.withType(ArtifactoryTask)
    }
  }

  /**
   * Name of maven java publication
   */
  public static final String MAVEN_JAVA_PUBLICATION_NAME = 'mavenJava'

  @PackageScope
  boolean createMavenJavaPublication = true

  private void configureArtifactsPublishing() {
    project.afterEvaluate {
      if (createMavenJavaPublication) {
        project.extensions.getByType(PublishingExtension).publications.register(MAVEN_JAVA_PUBLICATION_NAME, MavenPublication) { MavenPublication publication ->
          publication.from project.components.getByName('java' /* TODO */)
        }
      }
    }
    project.extensions.getByType(SigningExtension).sign project.extensions.getByType(PublishingExtension).publications

    configureArtifactory()
  }

  private void configureMavenCentral() {
    project.extensions.getByType(PublishingExtension).repositories.maven { MavenArtifactRepository mavenArtifactRepository ->
      mavenArtifactRepository.with {
        /*
         * WORKAROUND:
         * Groovy bug?
         * When GString is used, URI property setter is called anyway, and we got cast error
         * <grv87 2018-06-26>
         */
        url = project.uri(
          project.rootProject.convention.getPlugin(RootProjectConvention).isRelease.get() ?
          'https://oss.sonatype.org/service/local/staging/deploy/maven2' :
          'https://oss.sonatype.org/content/repositories/snapshots'
        )
        credentials.username = project.rootProject.extensions.extraProperties['mavenCentralUsername'].toString()
        credentials.password = project.rootProject.extensions.extraProperties['mavenCentralPassword'].toString()
      }
    }
    project.rootProject.tasks.named(RELEASE_TASK_NAME).configure { Task release ->
      release.finalizedBy PUBLISH_LIFECYCLE_TASK_NAME
    }
  }

  @SuppressWarnings(['UnnecessaryObjectReferences'])
  private void configureBintray() {
    RootProjectConvention rootProjectConvention = project.rootProject.convention.getPlugin(RootProjectConvention)
    ProjectConvention projectConvention = project.convention.getPlugin(ProjectConvention)
    project.pluginManager.apply 'com.jfrog.bintray'

    project.extensions.configure(BintrayExtension) { BintrayExtension extension ->
      extension.with {
        user = project.rootProject.extensions.extraProperties['bintrayUser'].toString()
        key = project.rootProject.extensions.extraProperties['bintrayAPIKey'].toString()
        pkg.repo = 'generic'
        pkg.name = 'gradle-project'
        pkg.userOrg = 'fidata'
        pkg.version.name = ''
        pkg.version.vcsTag = '' // TODO
        pkg.version.gpg.sign = true // TODO ?
        pkg.desc = project.version.toString() == '1.0.0' ? project.description : rootProjectConvention.changeLogTxt.get().toString()
        pkg.labels = projectConvention.tags.get().toArray(new String[0])
        pkg.licenses = [projectConvention.license].toArray(new String[1])
        pkg.vcsUrl = rootProjectConvention.vcsUrl.get()
        // pkg.version.attributes // Attributes to be attached to the version
      }
    }
    project.tasks.withType(BintrayPublishTask).configureEach { BintrayPublishTask bintrayPublish ->
      bintrayPublish.onlyIf { rootProjectConvention.isRelease.get() }
    }
    project.rootProject.tasks.named(RELEASE_TASK_NAME).configure { Task release ->
      release.finalizedBy project.tasks.withType(BintrayPublishTask)
    }
  }

  private void configureGithubReleases() {
    GitRepo repo = project.extensions.getByType(SemanticReleasePluginExtension).repo
    project.afterEvaluate {
      /**
       * CRED:
       * Code borrowed from JFrog Artifactory (build-info) Gradle plugin
       * under Apache 2.0 license
       * <grv87 2019-04-07>
       */
      project.extensions.getByType(PublishingExtension).publications.configureEach { Publication publication ->
        if (MavenPublicationInternal.isInstance(publication)) {
          MavenPublicationInternal mavenPublicationInternal = (MavenPublicationInternal) publication
          MavenNormalizedPublication mavenNormalizedPublication = mavenPublicationInternal.asNormalisedPublication()

          Set<MavenArtifact> artifacts = mavenNormalizedPublication.allArtifacts
          for (MavenArtifact artifact : artifacts) {
            File file = artifact.file
            repo.releaseAsset([
              label: artifact.classifier
            ], file)
          }
        } else if (IvyPublicationInternal.isInstance(publication)) {
          IvyPublicationInternal ivyPublicationInternal = (IvyPublicationInternal) publication
          IvyNormalizedPublication ivyNormalizedPublication = ivyPublicationInternal.asNormalisedPublication()

          Set<IvyArtifact> artifacts = ivyNormalizedPublication.allArtifacts
          for (IvyArtifact artifact : artifacts) {
            File file = artifact.file
            repo.releaseAsset([
              label: artifact.classifier
            ], file)
          }
        } else {
          throw new UnsupportedOperationException("Unsupported publication of type ${ publication.class.canonicalName }, named $publication.name")
        }
      }
    }
  }

  private void configureDocumentation() {
    if ([project.configurations.getByName(COMPILE_CONFIGURATION_NAME), project.configurations.getByName(API_CONFIGURATION_NAME)].any { Configuration configuration ->
      configuration.dependencies.contains(project.dependencies.gradleApi())
    }) {
      project.extensions.getByType(JvmBaseExtension).javadocLinks['org.gradle'] = project.uri("https://docs.gradle.org/${ project.gradle.gradleVersion }/javadoc/index.html?")
    }

    project.tasks.withType(Javadoc).configureEach { Javadoc javadoc ->
      javadoc.options.encoding = UTF_8.name()
      javadoc.doFirst {
        javadoc.options { StandardJavadocDocletOptions options ->
          javadoc.project.extensions.getByType(JvmBaseExtension).javadocLinks.values().each { URI link ->
            options.links link.toString()
          }
        }
      }
      if (!project.rootProject.convention.getPlugin(RootProjectConvention).isRelease.get()) {
        ((StandardJavadocDocletOptions)javadoc.options).noTimestamp = true
      }
    }
  }

  /**
   * Name of FindBugs common task
   */
  public static final String FINDBUGS_TASK_NAME = 'findbugs'

  /**
   * Name of JDepend common task
   */
  public static final String JDEPEND_TASK_NAME = 'jdepend'

  private void configureCodeQuality() {
    project.plugins.getPlugin(ProjectPlugin).addCodeQualityCommonTask 'FindBugs', FINDBUGS_TASK_NAME, FindBugs
    project.plugins.getPlugin(ProjectPlugin).addCodeQualityCommonTask 'JDepend', JDEPEND_TASK_NAME, JDepend
  }
}
