gradle-base-plugins
====================

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

`build`, `check` and `clean` tasks are provided by applied
`lifecycle-base` plugin. In original `lifecycle-base` plugin
`build` task depends on `check` task.
This plugin changes this behavior.

`check` task depends on all `Test` tasks and new `lint` task.

`release` task is provided by applied
[`de.gliderpilot.semantic-release` plugin](https://github.com/FIDATA/gradle-semantic-release-plugin).

### Build Tools Lifecycle

Plugin provides tasks for dealing with build tools managed by Gradle,
Bundler, NPM and similar tools:

*	`buildToolsInstall` - install build tools once after project clone.

*	`buildToolsUpdate` - updates build tools that could be updated
	automatically.

	This task should be run with `--write-locks` parameter.
	We could set this parameter in plugin, but this still wouldn't work
	for `buildSrc` projects.

*	`buildToolsOutdated` - shows outdated build tools that could not be
	updated automatically and/or have version restrictions. Developer
	have to update them manually.

The following plugins are used:

*	[`com.github.ben-manes.versions` plugin](https://github.com/ben-manes/gradle-versions-plugin)

	There is a missing feature in this plugin: `dependencyUpdates` task doesn't fail
	whenever dependencies are not up-to-date.
	See https://github.com/ben-manes/gradle-versions-plugin/issues/191
	and https://github.com/ben-manes/gradle-versions-plugin/issues/192
	for discussion.
	This will be resolved in the future.

Plugin also configures dependency resolution changing
[Ivy status](http://ant.apache.org/ivy/history/latest-milestone/terminology.html#status)
from `release` to `milestone` for artifacts having pre-release labels
in version.

### Documentation

*	Applies and configures
	[`org.ajoberstar.git-publish` plugin](https://github.com/ajoberstar/gradle-git-publish).
	allowing to publish documentation to GitHub pages.

*	Provides `noJekyll` task that generates `.nojekyll` file to
	[turn off Jekyll processing](https://github.com/blog/572-bypassing-jekyll-on-github-pages).

### Code Quality

*	Provides `lint` task

*	Applies
[`codenarc` plugin](https://docs.gradle.org/current/userguide/codenarc_plugin.html)

*	Provides `codenarc` task that runs all CodeNarc tasks.
	Includes this task in execution list for `lint` task.

*	Provides `codenarcBuildSrc` task for `build.gradle` itself
	and accompanying Groovy scripts.

*	Sets default configuration for all `codenarc` tasks.
	Adds `disabledRules` property to each task, so that specific rules
	could be disabled per task

### Reports

*	Provides read-only project properties:
	*	`reportsDir`
	*	`htmlReportsDir`
	*	`xmlReportsDir`
	*	`txtReportsDir`

*	Applies and configures `reporting-base` plugin. Redirects
	all reports	to `build/reports/<format>` directory.

	<BLOCKED: grv87 2018-06-19>
	Known limitation: `gradle --profile` reports are not redirected.
	They stay in `build/reports/profile` directory.
	See https://github.com/FIDATA/gradle-base-plugins/issues/1.

### Build Diagnostics and Troubleshooting

*	Applies plugins:
	*	[`project-report`](https://docs.gradle.org/current/userguide/project_reports_plugin.html)

		Provides `projectReport` and other tasks

	*	[`cz.malohlava`](https://github.com/mmalohlava/gradle-visteg)

		Generates dependency graph in Graphviz format

	*	[`com.dorongold.task-tree`](https://github.com/dorongold/gradle-task-tree)

		Provides `taskTree` task

*	Provides `inputsOutputs` task which generates reports about all task
	file inputs and outputs.

All these tasks are put into `Diagnostics` group.

### Other features

*	Sets `group` to `org.fidata` if it hasn't been set already.

*	Provides `license` property used by other plugins.
	[SPDX license identifiers](https://spdx.org/licenses/)
	are supported.

*	Applies
	[`nebula.contacts` plugin](https://github.com/nebula-plugins/gradle-contacts-plugin)

	Provides `contacts` extension.

*	Applies and configures
	[`com.jfrog.artifactory` plugin](https://www.jfrog.com/confluence/display/RTF/Gradle+Artifactory+Plugin)

	Allows us to resolve artifacts from
	[FIDATA Artifactory](https://artifactory.fidata.org/).

*	Applies
	[`signing` plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)

*	Provides read-only `isRelease` and `changeLog` project properties
	for work with semantic release.

*	Provides `publicReleases` project property used by other plugins.

	Setting it to true turns on all public-release tasks: publishing
	artifacts to Maven Central, JCenter and so on.

	By default it is false.

*	Configures `wrapper` task to specific Gradle version.

### Supported Gradle and JDK versions:

*	Requires Gradle >= 4.8
*	Built and tested with JDK 8

## `org.fidata.project.jdk` plugin

JDK-based project.

Applies [`org.fidata.project` plugin](#orgfidataproject-plugin),
and also:

*	Applies
	[`java`](https://docs.gradle.org/current/userguide/java_plugin.html)
	and
	[`java-library`](https://docs.gradle.org/current/userguide/java_library_plugin.html)
	plugins

*	Adds license file into JAR `META-INF` directory

*	Adds [JUnit](http://junit.org/junit4/) dependency
	to `testImplementation` configuration

*	Adds and configures `functionalTest` task (framework-agnostic)

*	Applies
	[`maven` plugin](https://docs.gradle.org/current/userguide/maven_plugin.html)

	Configures Maven publication to Artifactory.

	If `publicReleases` is on — configures publication to Maven Central.

*	If `publicReleases` is on — applies
	[`com.jfrog.bintray` plugin](https://github.com/bintray/gradle-bintray-plugin)
	and configures publication to JCenter

*	Applies `javadoc.io-linker` plugin

*	Adds `javadoc` output to GitHub Pages publication

## `org.fidata.project.groovy` plugin

Groovy language project.

Applies [`org.fidata.project.jdk` plugin](#orgfidataprojectjdk-plugin),
and also:

*	Applies
	[`groovy` plugin](https://docs.gradle.org/current/userguide/groovy_plugin.html)

	Adds Groovy to `api` configuration.

*	Adds [Spock](https://spockframework.org/) dependency
	to `testImplementation` configuration for functional testing

	Configures `functionalTest` task to use Spock.

*	Adds
	[Spock Reports](https://github.com/renatoathaydes/spock-reports)
	dependency to `testRuntimeOnly` configuration

*	Configures GroovyDoc:
	*	Adds links to Java SE API documentation
	*	Adds `groovydoc` output to GitHub Pages publication

## `org.fidata.plugin` plugin

Gradle plugin project.

This plugin depends on at least one of JDK-based project plugins:
*	[`org.fidata.project.jdk`](#orgfidataprojectjdk-plugin)
*	[`org.fidata.project.groovy`](#orgfidataprojectgroovy-plugin)

or others developed later.

They have to be applied manually depending on the language(s)
used in the project.

*	Applies
	[`java-gradle-plugin` plugin](https://docs.gradle.org/current/userguide/javaGradle_plugin.html)

*	Applies
	[`org.ajoberstar.stutter`](https://github.com/ajoberstar/gradle-stutter)
	and
	[`org.ysb33r.gradletest`](https://ysb33r.github.io/gradleTest/)
	plugins

	Allows us to test plugins under several different Gradle versions

*	Adds
	[Gradle TestKit](https://docs.gradle.org/current/userguide/test_kit.html)
	dependency to `testImplementation` configuration

*	If `publicReleases` is on — applies
	[`com.gradle.plugin-publish` plugin](https://plugins.gradle.org/docs/publish-plugin)

*	Configures `groovydoc` links to Gradle API

# Properties

Provided via `gradle.properties` file.

Property | Usage | Notes
---------|-------|------
artifactoryUser     <td rowspan="2"> Getting build tools and dependencies from Artifactory; use Gradle cache |
artifactoryPassword | It is actually API key
gitUsername <td rowspan="2"> Git push during release |
gitPassword |
ghToken | Create release on GitHub |
gpgKeyId                <td rowspan="2"> Sign artifacts, git commits and git tags |
gpgKeySecretKeyRingFile |
bintrayUser   <td rowspan="2"> Release to Bintray |
bintrayAPIKey |

# Development

This is self-applying plugin. That means that build script requires
the plugin itself (just compiled, not released to the repository).
So, if there are any errors during compilation or plugin applying,
Gradle build script just doesn't work.
If it is a compilation error, you can run `../gradlew build`
in `buildSrc` directory to figure out what's going on.


------------------------------------------------------------------------
Copyright © 2017-2018  Basil Peace

This file is part of gradle-base-plugins.

Copying and distribution of this file, with or without modification,
are permitted in any medium without royalty provided the copyright
notice and this notice are preserved.  This file is offered as-is,
without any warranty.
