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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.fidata.gradle;

import lombok.Getter;
import org.fidata.gradle.internal.AbstractExtension;
import org.fidata.gradle.utils.PathDirector;
import org.gradle.api.Project;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension;
import com.github.zafarkhaja.semver.Version;
import de.gliderpilot.gradle.semanticrelease.SemanticReleasePluginExtension;
import de.gliderpilot.gradle.semanticrelease.SemanticReleaseChangeLogService;
import org.ajoberstar.gradle.git.release.base.ReleaseVersion;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import groovy.lang.Writable;

/**
 * Provides additional properties to the project
 */
public class ProjectConvention extends AbstractExtension {
  private final Project project;

  /**
   * @return list of tags for the project
   */
  @Getter
  private final ListProperty<String> tags;

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
  public final void setLicense(final String newValue) throws InvalidLicenseStringException {
    String oldLicense = license;
    license = newValue;
    if (!LicenseInfoFactory.isSpdxListedLicenseID(newValue)) {
      throw new InvalidLicenseStringException(String.format("License identifier is not in SPDX list: %s", newValue));
    }
    getPropertyChangeSupport().firePropertyChange("license", oldLicense, newValue);
  }

  /**
   * @return whether releases of this project are public
   */
  @Getter
  private boolean publicReleases = false;

  /**
   * Sets whether releases of this project are public
   * @param newValue whether releases of this project are public
   */
  public final void setPublicReleases(final boolean newValue) {
    boolean oldValue = publicReleases;
    publicReleases = newValue;
    getPropertyChangeSupport().firePropertyChange("publicReleases", new Boolean(oldValue), new Boolean(newValue));
  }

  /**
   * @return whether this run has release version (not snapshot)
   */
  @Getter
  private final Provider<Boolean> isRelease;

  /**
   * @return changelog since last release
   */
  @Getter
  private final Provider<Writable> changeLog;

  /**
   * @return changelog since last release in text format
   */
  @Getter
  private final Provider<Writable> changeLogTxt;

  /**
   * @return project website URL
   */
  @Getter
  private final Property<String> websiteUrl;

  /**
   * @return issues URL
   */
  @Getter
  private final Property<String> issuesUrl;

  /**
   * @return project VCS URL
   */
  public final Provider<String> vcsUrl;

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
   * @return output directory for JSON reports
   */
  @Getter
  private final File jsonReportsDir;

  /**
   * @return output directory for text reports
   */
  @Getter
  private final File txtReportsDir;

  public ProjectConvention(final Project project) {
    super();

    this.project = project;

    tags = project.getObjects().listProperty(String.class);

    /*
     * WORKAROUND:
     * We can't use lambda expressions since they are not supported by Groovydoc yet
     * https://issues.apache.org/jira/browse/GROOVY-7013
     * <grv87 2018-08-01>
     */
    isRelease = project.provider(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return !project.getVersion().toString().endsWith("-SNAPSHOT");
      }
    });

    changeLog = project.provider(new Callable<Writable>() {
      @Override
      public Writable call() throws Exception {
        SemanticReleaseChangeLogService changeLogService = project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
        Object version = project.getVersion();
        ReleaseVersion inferredVersion = ((ReleasePluginExtension.DelayedVersion)version).getInferredVersion();
        return changeLogService.getChangeLog().call(changeLogService.commits(Version.valueOf(inferredVersion.getPreviousVersion())), inferredVersion);
      }
    });

    changeLogTxt = project.provider(new Callable<Writable>() {
      @Override
      public Writable call() throws Exception {
        SemanticReleaseChangeLogService changeLogService = project.getExtensions().getByType(SemanticReleasePluginExtension.class).getChangeLog();
        Object version = project.getVersion();
        ReleaseVersion inferredVersion = ((ReleasePluginExtension.DelayedVersion)version).getInferredVersion();
        return changeLogService.getChangeLogTxt().call(changeLogService.commits(Version.valueOf(inferredVersion.getPreviousVersion())), inferredVersion);
      }
    });

    vcsUrl = project.provider(new Callable<String>() {
      @Override
      public String call() throws Exception {
        return "https://github.com/FIDATA/" + project.getName();
      }
    });
    websiteUrl = project.getObjects().property(String.class);
    websiteUrl.set(vcsUrl);
    issuesUrl = project.getObjects().property(String.class);
    issuesUrl.set(websiteUrl + "/issues");

    reportsDir = new File(project.getBuildDir(), "reports");
    htmlReportsDir = new File(reportsDir, "html");
    xmlReportsDir  = new File(reportsDir, "xml");
    jsonReportsDir = new File(reportsDir, "json");
    txtReportsDir  = new File(reportsDir, "txt");
  }

  /**
   * Returns directory
   * inside standard directory for HTML reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  public File getHtmlReportDir(Path subpath) {
    return htmlReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for XML reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  public File getXmlReportDir(Path subpath) {
    return xmlReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for JSON reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  public File getJsonReportDir(Path subpath) {
    return jsonReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for text reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  public File getTxtReportDir(Path subpath) {
    return txtReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for HTML reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getHtmlReportDir(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return htmlReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for XML reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getXmlReportDir(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return xmlReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for JSON reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getJsonReportDir(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return jsonReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for text reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getTxtReportDir(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return txtReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for HTML reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getHtmlReportDir(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return htmlReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for XML reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getXmlReportDir(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return xmlReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for JSON reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getJsonReportDir(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return jsonReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for text reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return Directory resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getTxtReportDir(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return txtReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for HTML reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getHtmlReportFile(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return htmlReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, "html")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for XML reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getXmlReportFile(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return xmlReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, "xml")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for JSON reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getJsonReportFile(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return jsonReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, "json")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for text reports
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getTxtReportFile(Path subpath, PathDirector<T> pathDirector, T object) throws RuntimeException {
    return txtReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, "txt")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for HTML reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getHtmlReportFile(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return htmlReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, "html")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for TXT reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getXmlReportFile(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return xmlReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, "xml")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for JSON reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getJsonReportFile(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return jsonReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, "json")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for text reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of {@object}
   * @return File resolved to the root of standard directory
   * @throws RuntimeException If {@code pathDirector} could not determine path
   */
  public <T> File getTxtReportFile(PathDirector<T> pathDirector, T object) throws RuntimeException {
    return txtReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, "txt")).toFile();
  }

  private <T> Path getFileNameWithExtension(PathDirector<T> pathDirector, T object, String extension) throws RuntimeException {
    Path filenameWithoutExtension = pathDirector.determinePath(object);
    return filenameWithoutExtension.resolveSibling(filenameWithoutExtension.getFileName().toString() + "." + extension);
  }
}
