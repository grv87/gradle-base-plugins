/*
 * ProjectConvention class
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

import org.fidata.gradle.internal.AbstractExtension;
import org.gradle.api.Project;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import lombok.Getter;
import lombok.Setter;
import java.io.File;
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension;
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService;
import org.ajoberstar.gradle.git.release.base.ReleaseVersion;
import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.ParseException;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.base.Splitter;
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import groovy.lang.Writable;
import java.lang.reflect.InvocationTargetException;
import groovy.lang.GString;

/**
 * Provides additional properties to the project
 */
public final class ProjectConvention extends AbstractExtension {
  /**
   * Whether this run has release version (not snapshot)
   */
  @Getter
  private final boolean isRelease;

  /**
   * Changelog since last release
   */
  @Getter
  private final @NonNull Writable changeLog;

  /**
   * Parent output directory for reports
   */
  @Getter // TOTEST: Annotation is copied to Getter
  private final @NonNull File reportsDir;

  /**
   * Output directory for HTML reports
   */
  @Getter
  private final @NonNull File htmlReportsDir;

  /**
   * Output directory for XML reports
   */
  @Getter
  private final @NonNull File xmlReportsDir;

  /**
   * Output directory for text reports
   */
  @Getter
  private final @NonNull File txtReportsDir;

  ProjectConvention(@NonNull Project project) {
    super();

    Object version = project.getVersion();
    isRelease = !version.toString().endsWith("-SNAPSHOT");
    SemanticReleaseChangeLogService changeLogService = project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
    /*
     * CAVEAT:
     * project.version is an instance of private class ReleasePluginExtension.DelayedVersion.
     * See https://github.com/ajoberstar/gradle-git/issues/272
     * We use Java reflection to get value of its fields
     * <grv87 2018-02-17>
     */
    Writable changeLog;
    try {
      ReleaseVersion inferredVersion = (ReleaseVersion)version.getClass().getField("inferredVersion").get(version);
      changeLog = changeLogService.getChangeLog().call(
        new Object[]{changeLogService.getClass()
        .getMethod("commits", new Class[]{Version.class})
        .invoke(changeLogService, new Object[]{Version
        .valueOf(inferredVersion.getPreviousVersion())}), inferredVersion});
    } catch (NoSuchFieldException|IllegalAccessException|NoSuchMethodException|InvocationTargetException e) {
      changeLog = GString.EMPTY;
      project.getLogger().error("%s: Can't get Project changelog", this.getClass().getName());
    }
    this.changeLog = changeLog;

    reportsDir = new File(project.getBuildDir(), "reports");
    xmlReportsDir = new File(reportsDir, "xml");
    htmlReportsDir = new File(reportsDir, "html");
    txtReportsDir = new File(reportsDir, "txt");
  }

  /**
   * SPDX identifier of the project license
   */
  @Getter
  private String license;

  /**
   * Sets the project license
   * @param newValue SPDX license identifier
   */
  public void setLicense(String newValue) throws InvalidLicenseStringException {
    String oldLicense = license;
    AnyLicenseInfo oldLicenseInfo = licenseInfo;
    license = newValue;
    this.licenseInfo = LicenseInfoFactory.parseSPDXLicenseString(license);
    propertyChangeSupport.firePropertyChange("license", oldLicense, newValue);
    propertyChangeSupport.firePropertyChange("licenseInfo", oldLicenseInfo, licenseInfo);
  }

  /**
   * Project license information
   */
  @Getter
  private AnyLicenseInfo licenseInfo;

  /**
   * Whether releases of this project are public
   */
  @Getter
  private boolean publicReleases = false;

  /**
   * Sets whether releases of this project are public
   */
  void setPublicReleases(boolean newValue) {
    boolean oldValue = publicReleases;
    publicReleases = newValue;
    propertyChangeSupport.firePropertyChange("publicReleases", oldValue, newValue);
  }

  static @Nullable Boolean isPreReleaseVersion(@Nullable String version) {
    if (Strings.isNullOrEmpty(version)) {
      return null;
    };
    try {
      return Version.valueOf(version).getPreReleaseVersion() != "";
    }
    catch (ParseException e) {
      return Iterables.any(Splitter.on(CharMatcher.anyOf("-\\._")).split(version), new Predicate<String>() {
        public boolean apply(String label) {
          label = label.toUpperCase();
          return label.startsWith("ALPHA") || label.startsWith("BETA") || label.startsWith("RC") || label.startsWith("CR") || label.startsWith("SNAPSHOT");
        }
      });
    }
  }
}
