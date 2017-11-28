#!/usr/bin/env groovy
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
package org.fidata.gradle

import org.fidata.gradle.internal.AbstractExtension
import org.gradle.api.Project
import javax.validation.constraints.NotNull
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService
import com.github.zafarkhaja.semver.Version
import org.spdx.rdfparser.license.LicenseInfoFactory
import org.spdx.rdfparser.license.AnyLicenseInfo

/**
 * Provides additional properties to the project
 */
final class ProjectConvention extends AbstractExtension {
  @NotNull
  private final boolean isRelease

  /**
   * Gets whether this run has release version (not snapshot)
   */
  @NotNull
  boolean getIsRelease() {
    isRelease
  }

  @NotNull
  private final String changeLog

  /**
   * Gets changelog since last release
   */
  @NotNull
  String getChangeLog() {
    changeLog
  }

  @NotNull
  private final File reportsDir

  /**
   * Gets parent output directory for reports
   */
  @NotNull
  File getReportsDir() {
    reportsDir
  }

  @NotNull
  private final File htmlReportsDir

  /**
   * Gets output directory for HTML reports
   */
  @NotNull
  File getHtmlReportsDir() {
    htmlReportsDir
  }

  @NotNull
  private final File xmlReportsDir

  /**
   * Gets output directory for XML reports
   */
  @NotNull
  File getXmlReportsDir() {
    xmlReportsDir
  }

  @NotNull
  private final File txtReportsDir

  /**
   * Gets output directory for text reports
   */
  @NotNull
  File getTxtReportsDir() {
    txtReportsDir
  }

  ProjectConvention(Project project) {
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

  private String license

  /**
   * Gets the project license
   * @return SPDX license identifier
   */
  String getLicense() {
    license
  }

  /**
   * Sets the project license
   * @param newValue SPDX license identifier
   */
  void setLicense(String newValue) {
    String oldLicense = license
    AnyLicenseInfo oldLicenseInfo = licenseInfo
    license = newValue
    licenseInfo = LicenseInfoFactory.parseSPDXLicenseString(license)
    propertyChangeSupport.firePropertyChange('license', oldLicense, newValue)
    propertyChangeSupport.firePropertyChange('licenseInfo', oldLicenseInfo, licenseInfo)
  }

  private AnyLicenseInfo licenseInfo

  /**
   * Gets the project license information
   */
  String getLicenseInfo() {
    licenseInfo
  }

  @NotNull
  private boolean publicReleases = false

  /**
   * Gets whether releases of this project are public
   */
  @NotNull
  boolean getPublicReleases() {
    publicReleases
  }

  /**
   * Sets whether releases of this project are public
   */
  void setPublicReleases(@NotNull boolean newValue) {
    boolean oldValue = publicReleases
    publicReleases = newValue
    propertyChangeSupport.firePropertyChange('publicReleases', oldValue, newValue)
  }
}
