/*
 * RootProjectConvention class
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
package org.fidata.gradle;

import static org.fidata.utils.VersionUtils.SNAPSHOT_SUFFIX;
import com.github.zafarkhaja.semver.Version;
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService;
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension;
import groovy.lang.Closure;
import groovy.lang.Writable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.Getter;
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension;
import org.ajoberstar.gradle.git.release.base.ReleaseVersion;
import org.ajoberstar.grgit.Commit;
import org.fidata.gradle.internal.AbstractExtension;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.cache.internal.filelock.LockFileAccess;
import org.gradle.cache.internal.filelock.LockStateAccess;
import org.gradle.cache.internal.filelock.Version1LockStateSerializer;

/**
 * Provides additional properties to the root project.
 */
public final class RootProjectConvention extends AbstractExtension {
  private static final String BUILD_SRC_PROJECT_CAN_T_HAVE_CHANGELOG = "buildSrc project can't have changelog";
  /**
   * Returns whether the project this convention is applied to is buildSrc.
   *
   * @return whether the project this convention is applied to is buildSrc
   */
  @Getter
  private final boolean isBuildSrc;

  private final Provider<Boolean> isRelease;

  /**
   * Returns whether this run has release version (not snapshot).
   *
   * @return whether this run has release version (not snapshot)
   */
  public Provider<Boolean> getIsRelease() {
    if (isBuildSrc) {
      throw new IllegalStateException("buildSrc project can't have releases at all");
    }
    return isRelease;
  }

  private final Provider<Writable> changeLog;

  /**
   * Returns changelog since last release in Markdown format.
   *
   * @return changelog since last release
   */
  public Provider<Writable> getChangeLog() {
    if (isBuildSrc) {
      throw new IllegalStateException(BUILD_SRC_PROJECT_CAN_T_HAVE_CHANGELOG);
    }
    return changeLog;
  }

  private final Provider<Writable> changeLogTxt;

  /**
   * Returns changelog since last release in text format.
   *
   * @return changelog since last release
   */
  public Provider<Writable> getChangeLogTxt() {
    if (isBuildSrc) {
      throw new IllegalStateException(BUILD_SRC_PROJECT_CAN_T_HAVE_CHANGELOG);
    }
    return changeLogTxt;
  }

  /**
   * Returns issues URL.
   *
   * @return issues URL
   */
  @Getter
  private final Property<String> issuesUrl;

  /**
   * Project VCS URL.
   *
   * @return project VCS URL
   */
  @Getter
  private final Provider<String> vcsUrl;

  /**
   * Default constructor.
   *
   * @param project the project this instance is being applied to
   */
  public RootProjectConvention(final Project project) {
    if (project != project.getRootProject()) {
      throw new IllegalStateException("RootProjectConvention can only be added to root project");
    }

    @SuppressWarnings({"LocalVariableHidesMemberVariable", "checkstyle:hiddenfield"})
    boolean isBuildSrc;
    try {
      isBuildSrc = "buildSrc".equals(project.getProjectDir().getName()) && new LockFileAccess(project.file(".gradle/noVersion/buildSrc.lock"), new LockStateAccess(new Version1LockStateSerializer())).readLockInfo().lockId != 0;
    } catch (final IOException ignored) {
      isBuildSrc = false;
    }
    this.isBuildSrc = isBuildSrc;

    if (!isBuildSrc) {
      /*
       * WORKAROUND:
       * We can't use lambda expressions since they are not supported by Groovydoc yet
       * https://issues.apache.org/jira/browse/GROOVY-7013
       * <grv87 2018-08-01>
       */
      isRelease = project.provider(new Callable<Boolean>() {
        @Override
        public Boolean call() {
          return !SNAPSHOT_SUFFIX.matcher(project.getVersion().toString()).find();
        }
      });

      changeLog = project.provider(new Callable<Writable>() {
        @Override
        public Writable call() {
          final SemanticReleaseChangeLogService changeLogService = getChangeLogService(project);
          return callChangeLog(project, changeLogService, changeLogService.getChangeLog());
        }
      });
      changeLogTxt = project.provider(new Callable<Writable>() {
        @Override
        public Writable call() {
          final SemanticReleaseChangeLogService changeLogService = getChangeLogService(project);
          return callChangeLog(project, changeLogService, changeLogService.getChangeLogTxt());
        }
      });

      vcsUrl = project.provider(new Callable<String>() {
        @Override
        public String call() {
          return "https://github.com/FIDATA/" + project.getName();
        }
      });
      issuesUrl = project.getObjects().property(String.class);
      issuesUrl.convention(vcsUrl + "/issues");
    } else {
      isRelease = null;

      changeLog = null;
      changeLogTxt = null;

      vcsUrl = null;
      issuesUrl = null;
    }
  }

  private SemanticReleaseChangeLogService getChangeLogService(final Project project) {
    return project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
  }

  private Writable callChangeLog(final Project project, final SemanticReleaseChangeLogService changeLogService, final Closure<Writable> changeLogClosure) {
    final ReleasePluginExtension.DelayedVersion version = (ReleasePluginExtension.DelayedVersion)project.getVersion();
    final ReleaseVersion inferredVersion = version.getInferredVersion();
    final String previousVersionString = inferredVersion.getPreviousVersion();
    final Version previousVersion = Version.valueOf(previousVersionString);
    final List<Commit> commits = changeLogService.commits(previousVersion);
    return changeLogClosure.call(commits, inferredVersion);
  }
}
