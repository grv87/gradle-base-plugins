/*
 * RootProjectConvention class
 * Copyright © 2017-2018  Basil Peace
 *
 * This file is part of gradle-base-plugins.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle;

import static org.fidata.gradle.utils.VersionUtils.SNAPSHOT_SUFFIX;
import com.github.zafarkhaja.semver.Version;
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService;
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension;
import groovy.lang.Writable;
import lombok.Getter;
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension;
import org.ajoberstar.gradle.git.release.base.ReleaseVersion;
import org.fidata.exceptions.InvalidOperationException;
import org.fidata.gradle.internal.AbstractExtension;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.cache.internal.filelock.LockFileAccess;
import org.gradle.cache.internal.filelock.LockStateAccess;
import org.gradle.cache.internal.filelock.Version1LockStateSerializer;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Provides additional properties to the root project
 */
public class RootProjectConvention extends AbstractExtension {
  /**
   * @return whether the project this convention is applied to is buildSrc
   */
  @Getter
  private final boolean isBuildSrc;

  private final Provider<Boolean> isRelease;

  /**
   * @return whether this run has release version (not snapshot)
   */
  public final Provider<Boolean> getIsRelease() {
    if (isBuildSrc) {
      throw new InvalidOperationException("buildSrc project can't have releases at all");
    }
    return isRelease;
  }

  private final Provider<Writable> changeLog;

  /**
   * @return changelog since last release
   */
  public final Provider<Writable> getChangeLog() {
    if (isBuildSrc) {
      throw new InvalidOperationException("buildSrc project can't have changelog");
    }
    return changeLog;
  }

  private final Provider<Writable> changeLogTxt;

  /**
   * @return changelog since last release in text format
   */
  public final Provider<Writable> getChangeLogTxt() {
    if (isBuildSrc) {
      throw new InvalidOperationException("buildSrc project can't have changelog");
    }
    return changeLogTxt;
  }

  /**
   * @return issues URL
   */
  @Getter
  private final Property<String> issuesUrl;

  /**
   * Project VCS URL
   */
  public final Provider<String> vcsUrl;

  public RootProjectConvention(final Project project) {
    super();

    if (project != project.getRootProject()) {
      throw new InvalidOperationException("RootProjectConvention can only be added to root project");
    }

    boolean _isBuildSrc;
    try {
      _isBuildSrc = "buildSrc".equals(project.getProjectDir().getName()) && new LockFileAccess(project.file(".gradle/noVersion/buildSrc.lock"), new LockStateAccess(new Version1LockStateSerializer())).readLockInfo().lockId != 0;
    } catch (IOException e) {
      _isBuildSrc = false;
    }
    isBuildSrc = _isBuildSrc;

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
          SemanticReleaseChangeLogService changeLogService = project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
          Object version = project.getVersion();
          ReleaseVersion inferredVersion = ((ReleasePluginExtension.DelayedVersion) version).getInferredVersion();
          return changeLogService.getChangeLog().call(changeLogService.commits(Version.valueOf(inferredVersion.getPreviousVersion())), inferredVersion);
        }
      });
      changeLogTxt = project.provider(new Callable<Writable>() {
        @Override
        public Writable call() {
          SemanticReleaseChangeLogService changeLogService = project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
          Object version = project.getVersion();
          ReleaseVersion inferredVersion = ((ReleasePluginExtension.DelayedVersion) version).getInferredVersion();
          return changeLogService.getChangeLogTxt().call(changeLogService.commits(Version.valueOf(inferredVersion.getPreviousVersion())), inferredVersion);
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
}
