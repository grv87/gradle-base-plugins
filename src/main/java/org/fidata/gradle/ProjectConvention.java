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

import lombok.Getter;
import lombok.Setter;
import org.fidata.gradle.internal.AbstractExtension;
import org.gradle.api.Project;
import java.io.File;
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension;
import com.github.zafarkhaja.semver.Version;
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension;
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService;
import org.ajoberstar.gradle.git.release.base.ReleaseVersion;
import org.spdx.rdfparser.license.SpdxNoneLicense;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import groovy.lang.Writable;

/**
 * Provides additional properties to the project
 */
public class ProjectConvention extends AbstractExtension {
  private final Project project;

  /**
   * @return whether this run has release version (not snapshot)
   */
  @Getter(lazy = true)
  private final boolean isRelease = calculateIsRelease();

  private boolean calculateIsRelease() {
    return !project.getVersion().toString().endsWith("-SNAPSHOT");
  }

  /**
   * @return changelog since last release
   */
  @Getter(lazy = true)
  private final /*TODOC: https://github.com/rzwitserloot/lombok/issues/1585*/ Writable changeLog = generateChangeLog();

  private Writable generateChangeLog() {
    SemanticReleaseChangeLogService changeLogService = project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
    Object version = project.getVersion();
    ReleaseVersion inferredVersion = ((ReleasePluginExtension.DelayedVersion)version).getInferredVersion();
    return changeLogService.getChangeLog().call(changeLogService.commits(Version.valueOf(inferredVersion.getPreviousVersion())), inferredVersion);
  }

  /**
   * @return parent output directory for reports
   */
  @Getter
  private final File reportsDir;

  /**
   * @return output directory for HTML reports
   */
  @Getter
  private final File htmlReportsDir;

  /**
   * @return output directory for XML reports
   */
  @Getter
  private final File xmlReportsDir;

  /**
   * @return output directory for text reports
   */
  @Getter
  private final File txtReportsDir;

  public ProjectConvention(Project project) {
    super();

    this.project = project;

    reportsDir = new File(project.getBuildDir(), "reports");
    xmlReportsDir = new File(reportsDir, "xml");
    htmlReportsDir = new File(reportsDir, "html");
    txtReportsDir = new File(reportsDir, "txt");

    websiteUrl = getVcsUrl();
    issuesUrl = websiteUrl + "/issues";
  }

  /**
   * @return SPDX identifier of the project license
   */
  @Getter
  private String license;

  /**
   * Sets the project license
   * @param newValue SPDX license identifier
   * @throws InvalidLicenseStringException when license parsing with SPDX failed
   */
  public void setLicense(String newValue) throws InvalidLicenseStringException {
    String oldLicense = license;
    AnyLicenseInfo oldLicenseInfo = licenseInfo;
    license = newValue;
    this.licenseInfo = LicenseInfoFactory.parseSPDXLicenseString(license);
    getPropertyChangeSupport().firePropertyChange("license", oldLicense, newValue);
    getPropertyChangeSupport().firePropertyChange("licenseInfo", oldLicenseInfo, licenseInfo);
  }

  /**
   * @return project license information
   */
  @Getter
  private SpdxListedLicense spdxListedLicense = new SpdxNoneLicense();

  /**
   * @return whether releases of this project are public
   */
  @Getter
  private boolean publicReleases = false;

  /**
   * Sets whether releases of this project are public
   * @param newValue whether releases of this project are public
   */
  public void setPublicReleases(boolean newValue) {
    boolean oldValue = publicReleases;
    publicReleases = newValue;
    getPropertyChangeSupport().firePropertyChange("publicReleases", new Boolean(oldValue), new Boolean(newValue));
  }

  /**
   * @param websiteUrl project website URL
   * @return project website URL
   */
  @Getter @Setter
  private String websiteUrl;

  /**
   * @param issuesUrl issues URL
   * @return issues URL
   */
  @Getter @Setter
  private String issuesUrl;


  /**
   * @return project VCS URL
   */
  public String getVcsUrl() {
    return "https://github.com/FIDATA/" + project.getName();
  }

}
