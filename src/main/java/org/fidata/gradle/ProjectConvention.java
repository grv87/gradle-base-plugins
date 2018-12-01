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
import org.fidata.exceptions.InvalidOperationException;
import org.fidata.gradle.internal.AbstractExtension;
import org.fidata.gradle.utils.PathDirector;
import org.gradle.api.Project;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;
import org.spdx.rdfparser.license.LicenseInfoFactory;

/**
 * Provides additional properties to the project
 */
public class ProjectConvention extends AbstractExtension {
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

  private final boolean isBuildSrc;

  private boolean publicReleases = false;

  /**
   * @return whether releases of this project are public
   */
  public final boolean getPublicReleases() {
    if (isBuildSrc) {
      throw new InvalidOperationException("buildSrc project can't have releases at all");
    }
    return publicReleases;
  }

  /**
   * Sets whether releases of this project are public
   * @param newValue whether releases of this project are public
   */
  public final void setPublicReleases(final boolean newValue) {
    if (isBuildSrc) {
      throw new InvalidOperationException("buildSrc project can't have releases at all");
    }
    boolean oldValue = publicReleases;
    publicReleases = newValue;
    getPropertyChangeSupport().firePropertyChange("publicReleases", oldValue, newValue);
  }

  /**
   * @return project website URL
   */
  @Getter
  private final Property<String> websiteUrl;

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

  private File getReportsDirForFormat(Project project, String format) {
    File result = new File(reportsDir, format);
    if (project != project.getRootProject()) {
      result = new File(result, project.getName());
    }
    return result;
  }

  public ProjectConvention(final Project project) {
    super();

    tags = project.getObjects().listProperty(String.class);

    isBuildSrc = project.getRootProject().getConvention().getPlugin(RootProjectConvention.class).getIsBuildSrc();

    if (!isBuildSrc) {
      websiteUrl = project.getObjects().property(String.class);
      websiteUrl.set(project.provider(new Callable<String>() {
        @Override
        public String call() {
          return "https://github.com/FIDATA/" + project.getRootProject().getName();
        }
      }));
    } else {
      websiteUrl = null;
    }

    reportsDir = new File(project.getRootProject().getBuildDir(), "reports");
    htmlReportsDir = getReportsDirForFormat(project, "html");
    xmlReportsDir = getReportsDirForFormat(project, "xml");
    jsonReportsDir = getReportsDirForFormat(project, "json");
    txtReportsDir = getReportsDirForFormat(project, "txt");
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
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getHtmlReportDir(Path subpath, PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getXmlReportDir(Path subpath, PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getJsonReportDir(Path subpath, PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getTxtReportDir(Path subpath, PathDirector<T> pathDirector, T object) {
    return txtReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for HTML reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getHtmlReportDir(PathDirector<T> pathDirector, T object) {
    return htmlReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for XML reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getXmlReportDir(PathDirector<T> pathDirector, T object) {
    return xmlReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for JSON reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getJsonReportDir(PathDirector<T> pathDirector, T object) {
    return jsonReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for text reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  public <T> File getTxtReportDir(PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getHtmlReportFile(Path subpath, PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getXmlReportFile(Path subpath, PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getJsonReportFile(Path subpath, PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getTxtReportFile(Path subpath, PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getHtmlReportFile(PathDirector<T> pathDirector, T object) {
    return htmlReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, "html")).toFile();
  }

  /**
   * Returns file
   * inside standard directory for XML reports
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getXmlReportFile(PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getJsonReportFile(PathDirector<T> pathDirector, T object) {
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
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  public <T> File getTxtReportFile(PathDirector<T> pathDirector, T object) {
    return txtReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, "txt")).toFile();
  }

  private <T> Path getFileNameWithExtension(PathDirector<T> pathDirector, T object, String extension) {
    Path filenameWithoutExtension = pathDirector.determinePath(object);
    return filenameWithoutExtension.resolveSibling(filenameWithoutExtension.getFileName().toString() + "." + extension);
  }
}
