/*
 * VersionUtils class
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
package org.fidata.utils;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilities to work with version strings and objects.
 */
@SuppressWarnings("parameterassignment")
public final class VersionUtils {
  /**
   * Suffix for snapshot version.
   */
  public static final Pattern SNAPSHOT_SUFFIX = Pattern.compile("-SNAPSHOT\\z");

  private static final CharMatcher SEPARATOR_MATCHER = CharMatcher.anyOf("-\\._");

  /**
   * Checks whether specified version is actually a pre-release version.
   * @param version Version
   * @return true when version is definitely pre-release
   *         null on empty or null version
   */
  @SuppressWarnings("UnusedReturnValue")
  public static Boolean isPreReleaseVersion(@SuppressWarnings("AssignmentToMethodParameter") String version) {
    if (Strings.isNullOrEmpty(version)) {
      return null;
    }
    //noinspection AssignmentToMethodParameter
    version = version.toUpperCase(Locale.ROOT);
    try {
      final String preReleaseVersion = Version.valueOf(version).getPreReleaseVersion();
      if (preReleaseVersion.isEmpty()) {
        return Boolean.FALSE;
      }
      return !Iterables.all(Splitter.on(SEPARATOR_MATCHER).split(preReleaseVersion), new Predicate<String>() {
        public boolean apply(final String label) {
          return
            label.startsWith("GA") ||
            label.startsWith("RELEASE") ||
            label.startsWith("MR") ||
            label.startsWith("SP") ||
            label.startsWith("SR") ||
            label.startsWith("FINAL") ||
            label.matches("^\\d+$");
        }
      });
    } catch (final ParseException e) {
      return Iterables.any(Splitter.on(SEPARATOR_MATCHER).split(version), new Predicate<String>() {
        public boolean apply(final String label) {
          return
            label.startsWith("DEV") ||
            label.startsWith("SNAPSHOT") ||
            label.startsWith("ALPHA") ||
            label.startsWith("BETA") ||
            label.startsWith("MILESTONE") ||
            label.matches("^[ABM]\\d+$") ||
            label.startsWith("RC") ||
            label.startsWith("CR");
        }
      });
    }
  }

  // Suppress default constructor for noninstantiability
  private VersionUtils() {
    throw new UnsupportedOperationException();
  }
}
