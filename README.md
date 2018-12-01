gradle-base-plugins
===================

Plugins to configure Gradle to build projects (including
other Gradle pluging) developed by FIDATA.
They provide reasonable defaults and sane environment
for all our projects.

These plugins are highly opinionated and for internal use only.
They are not to be published to Maven Central or Gradle Plugins portal.
However, you are free to fork, modify or use it as an example
according to [Apache v2.0 license](LICENSE.txt).

If you are contributing to other FIDATA plugins the best choice is
to join [FIDATA organization](https://github.com/FIDATA)
and use all available infrastructure
including [our Artifactory repository](https://artifactory.fidata.org/)
where these plugins live.

## `org.fidata.project` plugin

General, language-agnostic project.

### Lifecycle

Basic build lifecycle is `assemble` → `check` → `release`.

`assemble`, `check` and, also, `clean` tasks are provided by applied
`lifecycle-base` plugin.

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

*   Adds Maven repository hosted on [FIDATA Artifactory
    ](https://artifactory.fidata.org/)

    For releases, it is always `-release` repository, so releases
    cannot have snapshot dependencies.
    Otherwise it is `-snapshot` repository, so snapshot versions
    could be used during development (but won't by default - see below).

*   Turns off changing modules caching, so that SNAPSHOT dependencies
    are updated on each run

*   Configures dependency resolution changing [Ivy status
    ](http://ant.apache.org/ivy/history/latest-milestone/terminology.html#status)
    from `release` to `milestone` for artifacts
    having pre-release labels in version

*   Adds property `status` to each `ExternalModuleDependency` instance.
    It should be used to configure desired status of dependency.
    By default all dependencies are resolved to `release`s, even
    if you use version ranges.
    If you want to get bleeding edge `SNAPSHOT` version you could use
    this property, like this:
    ```
    dependencies {
      compile('com.example:next-generation-library:[1, 2[').status = 'integration'
    }
    ```

    Of course, if there is more recent `release`
    with appropriate version then it will be used
    instead of old `SNAPSHOT`.

    Custom status schemes are not supported.

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

*	Provides `codenarc` and `pmd` tasks that run all PMD and CodeNarc
    tasks respectively.

	Includes these tasks in execution list for `lint` task.

*	Provides `codenarcBuildSrc` task for `build.gradle` itself
	and accompanying Groovy scripts

*	Sets default configuration for all `codenarc` tasks

	Adds `disabledRules` property to each task, so that specific rules
	could be disabled per task.

### Artifacts Publishing

*	Applies [`signing` plugin
	](https://docs.gradle.org/current/userguide/signing_plugin.html)

	By default, Java-based implementation of PGP is used. Secret keyring
	should be placed at GnuPG home in `secring.gpg` file.

	If you want to use GnuPG for signing,
	properties for this are already set.
	Switch can be made with [`signing.useGpgCmd()`
	](https://docs.gradle.org/current/userguide/signing_plugin.html#example_sign_with_gnupg).

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

*	Provides `inputsOutputs` task which generates reports about all task
	file inputs and outputs

All these tasks are put into `Diagnostics` group.

### Other features

*	Applies and configures [`com.jfrog.artifactory` plugin
    ](https://www.jfrog.com/confluence/display/RTF/Gradle+Artifactory+Plugin)

    Allows us to publish artifacts and build info to [FIDATA Artifactory
    ](https://artifactory.fidata.org/).

*	Sets project's `group` to `org.fidata` if it hasn't been set already

*	Provides `license` property used by other plugins

	It should be set with [SPDX license identifier
	](https://spdx.org/licenses/).

*   Provides `generateChangelog` and `generateChangelogTxt` tasks
    that generate changelog in Markdown and text formats
    in `build/changelog` directory

    The main usage is to check generated changelog
    during release preparation to make sure that everything is correct.

*   Provides `tags` property used by other plugins

*	Applies [`nebula.contacts` plugin
	](https://github.com/nebula-plugins/gradle-contacts-plugin)

	Provides `contacts` extension.

### buildSrc projects

Plugin can be applied to buildSrc projects.
However, buildSrc projects can't have releases and documentation,
so all related features are turned off.
They also don't publish build info to Artifactory.

They could have code quality and diagnostic tasks, but usage of them
is discouraged. Note that all buildSrc's Gradle and Groovy scripts
are already covered by `codenarcBuildSrc` task.

Project gets `isBuildSrc` read-only property which will be set to true
when buildSrc project is detected.

### Supported tools versions:

*	Requires Gradle >= 5.0

*	Built and tested with JDK 8

*   Requires GnuPG >= 2.1

    `gpg-agent` should have `allow-preset-passphrase` option turned on
    if GPG key with passphrase is used.
    It is usually achieved by adding this string
    into `gpg-agent.conf` file in GPG home directory.

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

### Code Quality

*	Provides `findbugs` and `jdepend` tasks that run all FindBugs
    and JDepend tasks respectively.

	Includes these tasks in execution list for `lint` task.

### Other features

*	Adds license file(s) into JAR `META-INF` directory

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

*	Provides `checkstyle` task that run all Checkstyle tasks.

	Includes this task in execution list for `lint` task.

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

*	Sets project's `group` to `org.fidata.gradle`
    if it hasn't been set already

# Properties

Should be provided in [standard Gradle ways
](https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties).

<table>
<thead><tr><th>Property</th><th>Requiring Plugin</th><th>Usage</th><th>Notes</th></tr></thead>
<tbody>
    <tr><td><code>artifactoryUser</code>    </td><td rowspan="7"><code>org.fidata.project</code></td><td rowspan="2">Getting build tools and dependencies from Artifactory; use Gradle cache</td><td>&nbsp;                </td>
    <tr><td><code>artifactoryPassword</code></td>                                                                                                                                                <td>It is actually API key</td>
    <tr><td><code>gitUsername</code></td>                                                            <td rowspan="2">Git push during release</td><td>&nbsp;</td>
    <tr><td><code>gitPassword</code></td>                                                                                                        <td>&nbsp;</td>
    <tr><td><code>ghToken</code></td>                                                                <td>Create release on GitHub</td><td>&nbsp;</td>
    <tr><td><code>gpgKeyId</code>        </td>                                                       <td rowspan="2">Sign artifacts, git commits and git tags</td><td>&nbsp;                                             </td>
    <tr><td><code>gpgKeyPassphrase</code></td>                                                                                                                    <td>Not required. Assumes no passphrase if not provided</td>
    <tr><td><code>bintrayUser</code>  </td><td rowspan="2"><code>org.fidata.base.jvm</code> for public releases</td><td rowspan="2">Release to Bintray</td><td>&nbsp;</td>
    <tr><td><code>bintrayAPIKey</code></td>                                                                                                                <td>&nbsp;</td>
    <tr><td><code>gradlePluginsKey</code>   </td><td rowspan="2"><code>org.fidata.plugin</code> for public releases</td><td rowspan="2">Release to Gradle Plugins portal</td><td>&nbsp;</td>
    <tr><td><code>gradlePluginsSecret</code></td>                                                                                                                            <td>&nbsp;</td>
</tbody>
</table>

All properties except `gpgKeyPassphrase` are required. The plugin
won't work if they are not set.

# Multi-project Builds (a.k.a Monorepo)
These plugins supports multi-project builds
in the following configuration:

1.  All child projects have the same version as the root one.
    This is the limitation
    imposed by `de.gliderpilot.semantic-release` plugin.

2.  `org.fidata.project` should be applied to root. It applies itself
    to each subproject

3.  `de.gliderpilot.semantic-release` and `org.ajoberstar.git-publish`
    plugins are applied to root only.

4.  The following properties are available for root project only:
    *   `isBuildSrc`
    *   `isRelease`
    *   `changeLog`
    *   `changeLogTxt`
    *   `issuesUrl`
    *   `vcsUrl`

    Except these, all other is configurable per project.

    Note that subprojects can have different licenses,
    and license file(s) (being included in JARs) are per project.

5.  Reports for all subprojects are redirected
    to `build/reports/<format>/<subproject>` directory.
    This is made for convenient usage under CI (Jenkins)

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

If a new version of a plugin won't be compatible
with previous Gradle versions:
1.  Update `ProjectPlugin.GRADLE_MINIMUM_SUPPORTED_VERSION` value
2.  Run `./gradlew stutterWriteLock`
3.  Update required Gradle version in this README file


------------------------------------------------------------------------
Copyright © 2017-2018  Basil Peace

This file is part of gradle-base-plugins.

Copying and distribution of this file, with or without modification,
are permitted in any medium without royalty provided the copyright
notice and this notice are preserved.  This file is offered as-is,
without any warranty.
