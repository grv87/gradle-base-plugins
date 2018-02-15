/*
 * org.fidata.plugin Gradle plugin
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

import static org.gradle.internal.FileUtils.toSafeFileName;
import org.fidata.gradle.internal.AbstractPlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.TaskContainer;
import com.gradle.publish.PublishPlugin;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.Action;
import org.gradle.plugin.devel.tasks.ValidateTaskProperties;
import org.gradle.api.tasks.javadoc.Groovydoc;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
// import org.gradle.api.publish.maven.MavenPublication;

/**
 * Provides an environment for a Gradle plugin project
 */
public final class GradlePluginPlugin extends AbstractPlugin implements PropertyChangeListener {
  @Override
  public void apply(Project project) {
    super.apply(project);
    PluginManager plugins = project.getPluginManager();

    plugins.apply(ProjectPlugin.class);

    for (String id : GradlePluginPluginDependencies.getDEFAULT_PLUGINS()) {
      plugins.apply(id);
    }

    project.getConvention().getPlugin(ProjectConvention.class).addPropertyChangeListener(this);
    configurePublicReleases();

    TaskContainer tasks = project.getTasks();

    tasks.withType(Test.class, new Action<Test>() {
      public void execute(Test task) {
        if (task.name.startsWith('compatTest')) {
          String reportDirName = "compatTest/${ toSafeFileName((name - ~/^compatTest/).uncapitalize()) }" /* uncapitalize requires Groovy >= 2.4.8, i.e. Gradle >= 3.5 */
          task.reports.junitXml.destination = new File(project.xmlReportsDir, reportDirName)
          if (task.project.plugins.hasPlugin(GroovyProjectPlugin)) {
            reports.html.enabled = false
            task.systemProperty('com.athaydes.spockframework.report.outputDir', new File(task.project.htmlReportsDir, "${ GroovyProjectPlugin.SPOCK_REPORTS_DIR_NAME }/$reportDirName").absolutePath)
          }
        }
      }
    })
      tasks.withType(ValidateTaskProperties) { ValidateTaskProperties task ->
        task.with {
          outputFile = new File(project.txtReportsDir, "${ toSafeFileName(name) }.txt")
          failOnWarning = true
        }
      }
      tasks.withType(Groovydoc) { Groovydoc task ->
        task.with {
          link "https://docs.gradle.org/${ project.gradle.gradleVersion }/javadoc/", 'org.gradle.'
        }
      }

      dependencies {
        api(gradleApi())
        testImplementation(gradleTestKit())
      }

      sourceSets {
        compatTest {
          compileClasspath += sourceSets.main.output + configurations.testCompileClasspath
          runtimeClasspath += output + compileClasspath + configurations.testRuntimeClasspath
        }
      }

      tasks['codenarcCompatTest'].ext.disabledRules = GroovyProjectPlugin.SPOCK_DISABLED_CODENARC_RULES

      if (project.hasProperty('artifactoryUser') && project.hasProperty('artifactoryPassword')) {
        String repositoryKey = isRelease ? 'plugins-release' : 'plugins-snapshot'
        artifactory {
          resolve {
            repository {
              repoKey = repositoryKey
            }
          }
          publish {
            repository {
              repoKey = isRelease ? 'plugins-release-local' : 'plugins-snapshot-local'
            }
          }
        }
      }
    }
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
    }
  }

  private void configurePublicReleases() {
    project.with {
      if (publicReleases) {
        plugins.apply 'com.gradle.plugin-publish'
        pluginBundle {
          website = "https://github.com/FIDATA/$name"
          vcsUrl = "https://github.com/FIDATA/$name"
          longDescription = changeLog
          mavenCoordinates {
            groupId = group
          }
        }
        tasks[PublishPlugin.PUBLISH_TASK_NAME].onlyIf { isRelease }
        tasks.release.finalizedBy publishPlugins
      }
    }
  }

}
