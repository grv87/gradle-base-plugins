/*
 * ProjectConvention class
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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.Getter;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.fidata.gradle.internal.AbstractExtension;
import org.fidata.gradle.utils.PathDirector;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.SpdxNoneLicense;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;

/**
 * Provides additional properties to the project.
 */
public final class ProjectConvention extends AbstractExtension {
  private static final String BUILD_SRC_PROJECT_CAN_T_HAVE_RELEASES_AT_ALL = "buildSrc project can't have releases at all";
  private static final String HTML = "html";
  private static final String XML = "xml";
  private static final String JSON = "json";
  private static final String TXT = "txt";

  /**
   * Returns list of tags for the project.
   *
   * @return list of tags for the project
   */
  @Getter
  private final ListProperty<String> tags;

  /**
   * Returns SPDX identifier of the project license.
   *
   * @return SPDX identifier of the project license
   */
  public String getLicense() {
    return spdxLicenseInfo.toString();
  }

  /**
   * Sets the project license from SPDX indentifier.
   *
   * @param newValue SPDX license identifier
   */
  public void setLicense(final String newValue) {
    try {
      setSpdxLicenseInfoInternal(LicenseInfoFactory.parseSPDXLicenseString(newValue));
    } catch (final InvalidLicenseStringException exception) {
      throw new IllegalArgumentException(String.format("Parsing license expression %s with SPDX failed", DefaultGroovyMethods.inspect(newValue)), exception);
    }
  }

  private AnyLicenseInfo spdxLicenseInfo = new SpdxNoneLicense();

  /**
   * Returns SPDX info on the project license.
   *
   * @return SPDX info on the project license
   */
  public AnyLicenseInfo getSpdxLicenseInfo() {
    return spdxLicenseInfo.clone();
  }

  /**
   * Sets SPDX info on the project license.
   *
   * @param newValue SPDX info on the project license
   */
  public void setSpdxLicenseInfo(final AnyLicenseInfo newValue) {
    setSpdxLicenseInfoInternal(newValue.clone());
  }

  private void setSpdxLicenseInfoInternal(final AnyLicenseInfo newValue) {
    final List<String> validationErrors = newValue.verify();
    if (!validationErrors.isEmpty()) {
      throw new IllegalArgumentException(String.format("License %s is not valid (for own developments). Validation errors: %s", DefaultGroovyMethods.inspect(newValue), DefaultGroovyMethods.toListString(validationErrors)));
    }
    final AnyLicenseInfo oldValue = spdxLicenseInfo;
    spdxLicenseInfo = newValue;
    getPropertyChangeSupport().firePropertyChange("spdxLicenseInfo", oldValue, newValue);
    getPropertyChangeSupport().firePropertyChange("license", oldValue.toString(), newValue.toString());
  }

  private final boolean isBuildSrc;

  private boolean publicReleases; // false by default

  /**
   * Returns whether releases of this project are public.
   *
   * @return whether releases of this project are public
   */
  public boolean getPublicReleases() {
    if (isBuildSrc) {
      throw new IllegalStateException(BUILD_SRC_PROJECT_CAN_T_HAVE_RELEASES_AT_ALL);
    }
    return publicReleases;
  }

  /**
   * Sets whether releases of this project are public.
   *
   * @param newValue whether releases of this project are public
   */
  public void setPublicReleases(final boolean newValue) {
    if (isBuildSrc) {
      throw new IllegalStateException(BUILD_SRC_PROJECT_CAN_T_HAVE_RELEASES_AT_ALL);
    }
    final boolean oldValue = publicReleases;
    publicReleases = newValue;
    getPropertyChangeSupport().firePropertyChange("publicReleases", oldValue, newValue);
  }

  /**
   * Returns project website URL.
   *
   * @return project website URL
   */
  @Getter
  private final Property<String> websiteUrl;

  /**
   * Returns parent output directory for reports.
   *
   * @return parent output directory for reports
   */
  @Getter
  private final File reportsDir;

  /**
   * Returns output directory for HTML reports.
   *
   * @return output directory for HTML reports
   */
  @Getter
  private final File htmlReportsDir;

  /**
   * Returns output directory for XML reports.
   *
   * @return output directory for XML reports
   */
  @Getter
  private final File xmlReportsDir;

  /**
   * Returns output directory for JSON reports.
   *
   * @return output directory for JSON reports
   */
  @Getter
  private final File jsonReportsDir;

  /**
   * Returns output directory for text reports.
   *
   * @return output directory for text reports
   */
  @Getter
  private final File txtReportsDir;

  private File getReportsDirForFormat(final Project project, final String format) {
    File result = new File(reportsDir, format);
    if (project != project.getRootProject()) {
      result = new File(result, project.getName());
    }
    return result;
  }

  /**
   * Default constructor.
   *
   * @param project the project this instance is being applied to
   */
  public ProjectConvention(final Project project) {
    tags = project.getObjects().listProperty(String.class).empty();

    isBuildSrc = project.getRootProject().getConvention().getPlugin(RootProjectConvention.class).getIsBuildSrc();

    if (!isBuildSrc) {
      websiteUrl = project.getObjects().property(String.class);
      websiteUrl.convention(project.provider(new Callable<String>() {
        @Override
        public String call() {
          return "https://github.com/FIDATA/" + project.getRootProject().getName();
        }
      }));
    } else {
      websiteUrl = null;
    }

    reportsDir = new File(project.getRootProject().getBuildDir(), "reports");
    htmlReportsDir = getReportsDirForFormat(project, HTML);
    xmlReportsDir = getReportsDirForFormat(project, XML);
    jsonReportsDir = getReportsDirForFormat(project, JSON);
    txtReportsDir = getReportsDirForFormat(project, TXT);
  }

  /**
   * Returns directory
   * inside standard directory for HTML reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public File getHtmlReportDir(final Path subpath) {
    return htmlReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for XML reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public File getXmlReportDir(final Path subpath) {
    return xmlReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for JSON reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public File getJsonReportDir(final Path subpath) {
    return jsonReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for text reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public File getTxtReportDir(final Path subpath) {
    return txtReportsDir.toPath().resolve(subpath).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for HTML reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getHtmlReportDir(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return htmlReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for XML reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getXmlReportDir(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return xmlReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for JSON reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getJsonReportDir(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return jsonReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for text reports.
   *
   * @param subpath Path relatively to the root of standard directory
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to {@code subpath}
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getTxtReportDir(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return txtReportsDir.toPath().resolve(subpath).resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for HTML reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getHtmlReportDir(final PathDirector<T> pathDirector, final T object) {
    return htmlReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for XML reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getXmlReportDir(final PathDirector<T> pathDirector, final T object) {
    return xmlReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for JSON reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getJsonReportDir(final PathDirector<T> pathDirector, final T object) {
    return jsonReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns directory
   * inside standard directory for text reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return Directory resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getTxtReportDir(final PathDirector<T> pathDirector, final T object) {
    return txtReportsDir.toPath().resolve(pathDirector.determinePath(object)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for HTML reports.
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
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getHtmlReportFile(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return htmlReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, HTML)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for XML reports.
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
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getXmlReportFile(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return xmlReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, XML)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for JSON reports.
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
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getJsonReportFile(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return jsonReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, JSON)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for text reports.
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
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getTxtReportFile(final Path subpath, final PathDirector<T> pathDirector, final T object) {
    return txtReportsDir.toPath().resolve(subpath).resolve(getFileNameWithExtension(pathDirector, object, TXT)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for HTML reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getHtmlReportFile(final PathDirector<T> pathDirector, final T object) {
    return htmlReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, HTML)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for XML reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getXmlReportFile(final PathDirector<T> pathDirector, final T object) {
    return xmlReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, XML)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for JSON reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getJsonReportFile(final PathDirector<T> pathDirector, final T object) {
    return jsonReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, JSON)).toFile();
  }

  /**
   * Returns file
   * inside standard directory for text reports.
   *
   * @param pathDirector Path director. Path provided by {@code pathDirector}
   *                     is resolved relatively to the root of standard directory.
   *                     Should return filename without extension.
   *                     Extension is added automatically
   * @param object The object to determine path for. It is passed to {@code pathDirector}
   * @param <T> Type of object
   * @return File resolved to the root of standard directory
   */
  @SuppressWarnings("overloadmethodsdeclarationorder")
  public <T> File getTxtReportFile(final PathDirector<T> pathDirector, final T object) {
    return txtReportsDir.toPath().resolve(getFileNameWithExtension(pathDirector, object, TXT)).toFile();
  }

  private <T> Path getFileNameWithExtension(final PathDirector<T> pathDirector, final T object, final String extension) {
    final Path filenameWithoutExtension = pathDirector.determinePath(object);
    return filenameWithoutExtension.resolveSibling(filenameWithoutExtension.getFileName().toString() + "." + extension);
  }
}
