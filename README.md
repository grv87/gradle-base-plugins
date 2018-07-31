gradle-base-plugins
===================

Base plugins for Gradle projects and plugins developed by FIDATA.
They provide reasonable defaults and sane environment
for all our projects.

These plugins are highly opinionated and for internal use only.
They are not to be published to Maven Central or Gradle Plugins portal.
However, you are free to fork, modify or use it as example
according to [Apache v2.0 license](LICENSE.txt).

If you are contributing to other FIDATA plugins the best choice is
to join [FIDATA organization](https://github.com/FIDATA)
and use all available infrastructure
including [our Artifactory repository](https://artifactory.fidata.org/)
where these plugins live.

## `org.fidata.project` plugin

General, language-agnostic project.

### Lifecycle

Basic development cycle is `build` → `check` → `release`.

`build`, `check` and, also, `clean` tasks are provided by applied
`lifecycle-base` plugin. In original `lifecycle-base` plugin
`build` task depends on `check` task.
This plugin changes this behavior.

`release` task is provided by applied [`de.gliderpilot.semantic-release`
plugin](https://github.com/FIDATA/gradle-semantic-release-plugin).

### Prerequisites Lifecycle

*   Applies [`org.fidata.prerequisites`
    ](https://github.com/FIDATA/gradle-prerequisites-plugin)
    providing tasks for dealing with prerequisites managed by Gradle,
    Bundler, NPM and similar tools.

    To report outdated Gradle dependencies
    [`com.github.ben-manes.versions` plugin
    ](https://github.com/ben-manes/gradle-versions-plugin) is used.

*   Configures `wrapper` task to specific Gradle version

### Dependency Resolution

*	Applies and configures [`com.jfrog.artifactory` plugin
    ](https://www.jfrog.com/confluence/display/RTF/Gradle+Artifactory+Plugin)

    Allows us to resolve artifacts from
    [FIDATA Artifactory](https://artifactory.fidata.org/), and also
    to publish build info there.

*   Turns off changing modules caching, so that SNAPSHOT dependencies
    are updated on each run

*   Configures dependency resolution changing [Ivy status
    ](http://ant.apache.org/ivy/history/latest-milestone/terminology.html#status)
    from `release` to `milestone` for artifacts
    having pre-release labels in version

### Documentation

*	Applies and configures [`org.ajoberstar.git-publish` plugin
    ](https://github.com/ajoberstar/gradle-git-publish)

	Allows to publish documentation to GitHub pages.

*	Provides `noJekyll` task that generates `.nojekyll` file to
	[turn off Jekyll processing
	](https://github.com/blog/572-bypassing-jekyll-on-github-pages)

### Code Quality

*	Provides `lint` task

    `check` task depends on all `Test` tasks and new `lint` task.

*	Applies [`codenarc` plugin
    ](https://docs.gradle.org/current/userguide/codenarc_plugin.html)

*	Provides `codenarc` task that runs all CodeNarc tasks

	Includes this task in execution list for `lint` task.

*	Provides `codenarcBuildSrc` task for `build.gradle` itself
	and accompanying Groovy scripts

*	Sets default configuration for all `codenarc` tasks

	Adds `disabledRules` property to each task, so that specific rules
	could be disabled per task.

### Artifacts Publishing

*	Applies [`signing` plugin
	](https://docs.gradle.org/current/userguide/signing_plugin.html)

*	Provides read-only `isRelease` and `changeLog` project properties
	for working with semantic release

*	Provides `publicReleases` project property used by other plugins

	Setting it to true turns on all public-release tasks: publishing
	artifacts to Maven Central, JCenter and so on.

	By default it is false.

### Reports

*	Provides read-only project properties:
	*	`reportsDir`
	*	`htmlReportsDir`
	*	`xmlReportsDir`
	*	`jsonReportsDir`
	*	`txtReportsDir`

*	Applies and configures `reporting-base` plugin

    Redirects all reports to `build/reports/<format>` directory.

	Known limitation: `gradle --profile` reports are not redirected.
	They stay in `build/reports/profile` directory for now.
	See https://github.com/FIDATA/gradle-base-plugins/issues/1

### Build Diagnostics and Troubleshooting

*	Applies plugins:
	*	[`project-report`
	    ](https://docs.gradle.org/current/userguide/project_reports_plugin.html)

		Provides `projectReport` and other tasks.

	*	[`com.dorongold.task-tree`
	    ](https://github.com/dorongold/gradle-task-tree)

		Provides `taskTree` task. It is wonderful in troubleshooting
		task dependencies.

	*	[`cz.malohlava`](https://github.com/mmalohlava/gradle-visteg)

		Generates task dependency graph in Graphviz format.

*	Provides `inputsOutputs` task which generates reports about all task
	file inputs and outputs

All these tasks are put into `Diagnostics` group.

### Other features

*	Sets project's `group` to `org.fidata` if it hasn't been set already

*	Provides `license` property used by other plugins

	It should be set with [SPDX license identifier
	](https://spdx.org/licenses/).

*   Provides `tags` property used by other plugins

*	Applies [`nebula.contacts` plugin
	](https://github.com/nebula-plugins/gradle-contacts-plugin)

	Provides `contacts` extension.

### Supported Gradle and JDK versions:

*	Requires Gradle >= 4.9
*	Built and tested with JDK 8

## `org.fidata.base.jvm` plugin

Project which uses JVM-based language.
This plugin should not be applied manually.

Applies [`org.fidata.project` plugin](#orgfidataproject-plugin),
and also:

*	Applies [`java-base`
    ](https://docs.gradle.org/current/userguide/java_plugin.html)
	and [`java-library`
	](https://docs.gradle.org/current/userguide/java_library_plugin.html)
	plugins

*   Provides `jvm` extension.
    This extension has one property `javadocLinks`.

    It contains links to external documentation
    used by `javadoc` and `groovydoc`.
    If a dependency is added automatically, its documentation is also
    added here automatically.
    Otherwise, you add link manually, like this:
    ```
    jvm.javadocLinks['com.example.super.cool.external.library'] = uri('https://example.com/javadoc/com/example/super.cool.external.library/1.0/')
    ```

*   Adds `mavenJava` publication (except when `org.fidata.plugin`
    is applied)

### Testing

*	Adds [JUnit](http://junit.org/junit4/) dependency
	to `testImplementation` configuration

*	Adds and configures `functionalTest` source set and task
    which uses [Spock framework](https://spockframework.org/)

	Also adds
	[Spock Reports](https://github.com/renatoathaydes/spock-reports).

	JUnit is also available whenever Spock is.

### Artifact Publishing

*	Applies [`maven-publish` plugin
    ](https://docs.gradle.org/current/userguide/publishing_maven.html)

	Configures Maven publication to Artifactory.

	Your `settings.gradle` should contain this row:
	```
	enableFeaturePreview('STABLE_PUBLISHING')
    ```

    to prevent the following error:

    ```
    org.gradle.api.InvalidUserDataException: Cannot configure the 'publishing' extension after it has been accessed.
    ```

	If `publicReleases` is on — configures publication to Maven Central.

*	If `publicReleases` is on — applies [`com.jfrog.bintray` plugin
	](https://github.com/bintray/gradle-bintray-plugin)
	and configures publication to JCenter

### Other features

*	Adds license file into JAR `META-INF` directory

## `org.fidata.project.java` plugin

Java language project.

Applies [`org.fidata.base.jvm` plugin](#orgfidatabasejvm-plugin),
and also:

*	Applies
	[`java`](https://docs.gradle.org/current/userguide/java_plugin.html)
	plugin

*   Applies [`io.franzbecker.gradle-lombok` plugin
    ](https://github.com/franzbecker/gradle-lombok) providing
    [Lombok](https://projectlombok.org/) for Java sources

*   Configures `javadoc` to parse sources through Delombok first

*   Adds `javadoc` output to GitHub Pages publication

## `org.fidata.base.groovy` plugin

Project which uses Groovy language.
This plugin should not be applied manually.

Applies [`org.fidata.base.jvm` plugin](#orgfidatabasejvm-plugin),
and also:

*	Applies [`groovy-base` plugin
    ](https://docs.gradle.org/current/userguide/groovy_plugin.html)

## `org.fidata.project.groovy` plugin

Groovy language project.

Applies [`org.fidata.base.groovy` plugin](#orgfidatabasegroovy-plugin),
and also:

*	Applies [`groovy` plugin
    ](https://docs.gradle.org/current/userguide/groovy_plugin.html)

*   Adds local Groovy to `api` configuration

*   Adds `groovydoc` output to GitHub Pages publication

## `org.fidata.plugin` plugin

Gradle plugin project.

This plugin depends on at least one of JVM-based project plugins:
*	[`org.fidata.project.java`](#orgfidataprojectjava-plugin)
*	[`org.fidata.project.groovy`](#orgfidataprojectgroovy-plugin)

or others developed later.

They have to be applied manually depending on the language(s)
used in the project.

*	Applies [`java-gradle-plugin` plugin
	](https://docs.gradle.org/current/userguide/javaGradle_plugin.html)

*	Configures `groovydoc` links to Gradle API

*	Adds [Gradle TestKit
    ](https://docs.gradle.org/current/userguide/test_kit.html)
	dependency to `functionalTest` source set configurations

*	Applies [`org.ysb33r.gradletest`
    ](https://ysb33rorg.gitlab.io/gradleTest/) and
	[`org.ajoberstar.stutter`
	](https://github.com/ajoberstar/gradle-stutter)	plugins

	Provides and configures `gradleTest` and `compatTest` source sets.
	Allows us to test plugins under several different Gradle versions.

*	If `publicReleases` is on — applies [`com.gradle.plugin-publish`
    plugin](https://plugins.gradle.org/docs/publish-plugin)

	`pluginBundle.plugins` should be configured manually. Other
	properties are set automatically.

*	Sets project's `group` to `org.fidata.gradle` if it hasn't been set already

# Properties

Should be provided via `gradle.properties` file.

<table>
<thead><tr><th>Property</th><th>Requiring Plugin</th><th>Usage</th><th>Notes</th></tr></thead>
<tbody>
    <tr><td><code>artifactoryUser</code>    </td><td rowspan="8"><code>org.fidata.project</code></td><td rowspan="2">Getting build tools and dependencies from Artifactory; use Gradle cache</td><td>&nbsp;                </td>
    <tr><td><code>artifactoryPassword</code></td>                                                                                                                                                <td>It is actually API key</td>
    <tr><td><code>gitUsername</code></td><td rowspan="2">Git push during release</td><td>&nbsp;</td>
    <tr><td><code>gitPassword</code></td>                                            <td>&nbsp;</td>
    <tr><td><code>ghToken</code></td><td>Create release on GitHub</td><td>&nbsp;</td>
    <tr><td><code>gpgKeyId</code>            </td><td rowspan="3">Sign artifacts, git commits and git tags</td><td>&nbsp;</td>
    <tr><td><code>gpgKeyPassword</code>      </td>                                                             <td>&nbsp;</td>
    <tr><td><code>gpgSecretKeyRingFile</code></td>                                                             <td>&nbsp;</td>
    <tr><td><code>bintrayUser</code>  </td><td rowspan="8"><code>org.fidata.base.jvm</code> for public releases</td><td rowspan="2">Release to Bintray</td><td>&nbsp;</td>
    <tr><td><code>bintrayAPIKey</code></td>                                                                                                                <td>&nbsp;</td>
</tbody>
</table>

# Development

This is self-applying plugin. That means that build script requires
the plugin itself (just compiled, not released to the repository).
So, if there are any errors during compilation or plugin applying,
Gradle build script just doesn't work.
If it is a compilation error, you can run `../gradlew build`
in `buildSrc` directory to figure out what's going on.

## Upgrading Gradle Version

Whenever new Gradle version is released, the way to upgrade is this:

1.  Read Release Notes and make necessary changes to the code
2.  Run `./gradlew stutterWriteLock`
3.  Run `./gradlew compatTest<new Gradle version>`
4.  Change Gradle version for `wrapper` task in plugin code
5.  Run `./gradlew wrapper && ./gradlew wrapper`


------------------------------------------------------------------------
Copyright © 2017-2018  Basil Peace

This file is part of gradle-base-plugins.

Copying and distribution of this file, with or without modification,
are permitted in any medium without royalty provided the copyright
notice and this notice are preserved.  This file is offered as-is,
without any warranty.
