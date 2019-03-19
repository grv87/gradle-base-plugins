/*
 * VersionUtils class
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
package org.fidata.gradle.utils;

import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.ParseException;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.base.Splitter;
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import java.util.Locale;

/**
 * Utilities to work with version strings and objects
 */
public final class VersionUtils {

  /**
   * Checks whether specified version is actually a pre-release version
   * @param version Version
   * @return true when version is definitely pre-release
   *         null on empty or null version
   */

  @SuppressWarnings("UnusedReturnValue")
  public static Boolean isPreReleaseVersion(String version) {
    if (Strings.isNullOrEmpty(version)) {
      return null;
    }
    try {
      String preReleaseVersion = Version.valueOf(version).getPreReleaseVersion();
      if (preReleaseVersion.isEmpty()) {
        return false;
      }
      return !Iterables.all(Splitter.on(CharMatcher.anyOf("-\\._")).split(preReleaseVersion), new Predicate<String>() {
        public boolean apply(String label) {
          label = label.toUpperCase(Locale.ROOT);
          return
            label.startsWith("GA") ||
            label.startsWith("RELEASE") ||
            label.startsWith("MR") ||
            label.startsWith("SP") ||
            label.startsWith("SR") ||
            label.startsWith("FINAL") ||
            label.matches("^\\d+$")
          ;
        }
      });
    }
    catch (ParseException e) {
      return Iterables.any(Splitter.on(CharMatcher.anyOf("-\\._")).split(version), new Predicate<String>() {
        public boolean apply(String label) {
          label = label.toUpperCase(Locale.ROOT);
          return
            label.startsWith("DEV") ||
            label.startsWith("SNAPSHOT") ||
            label.startsWith("ALPHA") ||
            label.startsWith("BETA") ||
            label.startsWith("MILESTONE") ||
            label.matches("^[ABM]\\d+$") ||
            label.startsWith("RC") ||
            label.startsWith("CR")
          ;
        }
      });
    }
  }

  // Suppress default constructor for noninstantiability
  private VersionUtils() {
    throw new UnsupportedOperationException();
  }
}
