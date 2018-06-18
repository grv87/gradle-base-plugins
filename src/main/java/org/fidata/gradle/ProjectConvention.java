/*
 * ProjectConvention class
 * Copyright © 2017-2018  Basil Peace
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
import com.github.zafarkhaja.semver.Version;
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension;
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService;
import org.ajoberstar.gradle.git.release.base.ReleaseVersion;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import groovy.lang.Writable;
import java.lang.reflect.InvocationTargetException;
import groovy.lang.GString;

/**
 * Provides additional properties to the project
 */
public class ProjectConvention extends AbstractExtension {
  private Project project;

  /**
   * Whether this run has release version (not snapshot)
   */
  @Getter
  private final boolean isRelease;

  /**
   * Changelog since last release
   */
  @Getter(lazy = true)
  private final /*@NonNull TODOC: https://github.com/rzwitserloot/lombok/issues/1585*/ Writable changeLog = generateChangeLog();

  // @CompileDynamic
  private Writable generateChangeLog() {
    SemanticReleaseChangeLogService changeLogService = project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
    Object version = project.getVersion();
    /*
     * CAVEAT:
     * project.version is an instance of private class ReleasePluginExtension.DelayedVersion.
     * See https://github.com/ajoberstar/gradle-git/issues/272
     * We use Java reflection to get value of its fields
     * <grv87 2018-02-17>
     */
    /*ReleaseVersion inferredVersion = ((ReleaseVersion)version).inferredVersion;
    return changeLogService.getChangeLog().call(changeLogService.commits(Version.valueOf(inferredVersion.getPreviousVersion())), inferredVersion);*/
    return null;
  }

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

  public ProjectConvention(@NonNull Project project) {
    super();

    this.project = project;

    isRelease = !project.getVersion().toString().endsWith("-SNAPSHOT");

    reportsDir = new File(project.getBuildDir(), "reports");
    xmlReportsDir = new File(reportsDir, "xml");
    htmlReportsDir = new File(reportsDir, "html");
    txtReportsDir = new File(reportsDir, "txt");

    websiteUrl = getVcsUrl();
    issuesUrl = websiteUrl + "/issues";
  }

  /**
   * SPDX identifier of the project license
   */
  @Getter
  private @Nullable String license;

  /**
   * Sets the project license
   * @param newValue SPDX license identifier
   */
  public void setLicense(@Nullable String newValue) throws InvalidLicenseStringException {
    String oldLicense = license;
    AnyLicenseInfo oldLicenseInfo = licenseInfo;
    license = newValue;
    this.licenseInfo = LicenseInfoFactory.parseSPDXLicenseString(license); // TODO: Error handling
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

  /**
   * Project website URL
   */
  @Getter @Setter
  private @NonNull String websiteUrl;

  /**
   * Issues URL
   */
  @Getter @Setter
  private @NonNull String issuesUrl;


  /**
   * Project VCS URL
   */

  public @NonNull String getVcsUrl() {
    return "https://github.com/FIDATA/" + project.getName();
  }

}
