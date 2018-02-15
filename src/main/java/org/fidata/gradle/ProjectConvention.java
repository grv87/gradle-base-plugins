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
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService;
import com.github.zafarkhaja.semver.Version;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.AnyLicenseInfo;

/**
 * Provides additional properties to the project
 */
public final class ProjectConvention extends AbstractExtension {
  private final boolean isRelease;

  /**
   * Gets whether this run has release version (not snapshot)
   */
  public boolean getIsRelease() {
    return isRelease;
  }

  private final @NonNull String changeLog;

  /**
   * Gets changelog since last release
   */
  @NonNull
  public String getChangeLog() {
    return changeLog
  }

  private final @NonNull File reportsDir;

  /**
   * Gets parent output directory for reports
   */
  @NonNull
  public File getReportsDir() {
    return reportsDir;
  }

  private final @NonNull File htmlReportsDir

  /**
   * Gets output directory for HTML reports
   */
  @NonNull
  public File getHtmlReportsDir() {
    return htmlReportsDir;
  }

  private final @NonNull File xmlReportsDir;

  /**
   * Gets output directory for XML reports
   */
  @NonNull
  public File getXmlReportsDir() {
    return xmlReportsDir;
  }

  private final @NonNull File txtReportsDir;

  /**
   * Gets output directory for text reports
   */
  @NonNull
  File getTxtReportsDir() {
    txtReportsDir
  }

  ProjectConvention(@NonNull Project project) {
    super()

    Object version = project.version
    isRelease = !version.toString().endsWith('-SNAPSHOT')
    SemanticReleaseChangeLogService changeLogService = project.semanticRelease.changeLog
    changeLog = changeLogService.changeLog(changeLogService.commits(Version.valueOf(version.inferredVersion.previousVersion)), version.inferredVersion)

    reportsDir = new File(project.buildDir, 'reports')
    xmlReportsDir = new File(reportsDir, 'xml')
    htmlReportsDir = new File(reportsDir, 'html')
    txtReportsDir = new File(reportsDir, 'txt')
  }

  private String license;

  /**
   * Gets the project license
   * @return SPDX license identifier
   */
  public String getLicense() {
    return license;
  }

  /**
   * Sets the project license
   * @param newValue SPDX license identifier
   */
  public void setLicense(String newValue) {
    String oldLicense = license;
    AnyLicenseInfo oldLicenseInfo = licenseInfo;
    license = newValue;
    this.licenseInfo = LicenseInfoFactory.parseSPDXLicenseString(license);
    propertyChangeSupport.firePropertyChange('license', oldLicense, newValue);
    propertyChangeSupport.firePropertyChange('licenseInfo', oldLicenseInfo, licenseInfo);
  }

  private AnyLicenseInfo licenseInfo;

  /**
   * Gets the project license information
   */
  AnyLicenseInfo getLicenseInfo() {
    return licenseInfo;
  }

  private boolean publicReleases = false;

  /**
   * Gets whether releases of this project are public
   */
  boolean getPublicReleases() {
    return publicReleases;
  }

  /**
   * Sets whether releases of this project are public
   */
  void setPublicReleases(boolean newValue) {
    boolean oldValue = publicReleases;
    publicReleases = newValue;
    propertyChangeSupport.firePropertyChange('publicReleases', oldValue, newValue);
  }
}
