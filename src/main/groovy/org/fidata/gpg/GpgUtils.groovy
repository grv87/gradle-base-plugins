#!/usr/bin/env groovy
/*
 * GpgUtils class
 * Copyright © 2018  Basil Peace
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
package org.fidata.gpg

// import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS
import org.gradle.api.Project
import org.gradle.process.ExecSpec
import java.security.InvalidKeyException
import groovy.transform.CompileStatic
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.Win32Exception
import java.nio.file.Path
import java.nio.file.Paths
import org.gradle.internal.os.OperatingSystem

/**
 * Utils to work with GPG
 *
 * TODO:
 * This should be implemented in separate library (in what language?)
 * accessible from e.g. Jenkins too
 */
@CompileStatic
final class GpgUtils {
  /**
   * Determines GPG home directory.
   * Doesn't check whether GPG is actually installed and doesn't try to run it.
   * if no explicit configuration is found it just assumes defaults
   *
   * @return GPG home directory
   */
  static final Path getGpgHome() {
    String gnupgHome = System.getenv('GNUPGHOME')
    if (gnupgHome != null) {
      return Paths.get(gnupgHome)
    }

    if (/*IS_OS_WINDOWS*/ OperatingSystem.current().windows) {
      try {
        String path = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, 'Software\\GNU\\GnuPG', 'HomeDir')
        if (path) {
          return Paths.get(path)
        }
      } catch (Win32Exception ignored) {
        /*
         * TODO:
         * We just assume that exception means that required registry key or value doesn't exist.
         * However, this could mean a lot of other problems.
         * We should check error codes and ignore non-existence only
         */
      }

      Path path = Paths.get(System.getenv('APPDATA'), 'GnuPg')
      if (!path.toFile().exists()) {
        Path path2 = Paths.get(System.getenv('USERPROFILE'), '.gnupg')
        if (path2.toFile().exists()) {
          // Old version of GnuPG under Windows (MinGW?) ? // TODO: log warning
          return path2
        }
      }
      return path
    }

    Paths.get(System.getenv('HOME'), '.gnupg')
  }

  /**
   * Determines GPG key grip
   * Runs GPG executable
   * @param project Gradle project
   * @param keyId GPG key id
   * @return keygrip
   * @throws InvalidKeyException when key was not found in GPG output
   * @throws IllegalStateException when there was other error parsing GPG output
   */
  @SuppressWarnings('DuplicateNumberLiteral')
  static final String getKeyGrip(Project project, String keyId) throws InvalidKeyException {
    String canonicalKeyId = keyId.toUpperCase()
    new ByteArrayOutputStream().withStream { os ->
      project.exec { ExecSpec execSpec ->
        execSpec.commandLine 'gpg', '--list-keys', '--with-colons', '--with-keygrip', keyId
        execSpec.standardOutput = os
      }
      String output = new String(os.toByteArray())
      List<String> lines = output.readLines()
      // See description of format at https://git.gnupg.org/cgi-bin/gitweb.cgi?p=gnupg.git;a=blob_plain;f=doc/DETAILS
      int i = 0
      boolean found
      // look for a key
      while (i < lines.size()) {
        String[] fields = lines[i].split(':')
        i++
        if (fields[0] == 'fpr' && fields[9].endsWith(canonicalKeyId)) {
          found = true
          break
        }
      }
      if (found) {
        while (i < lines.size()) {
          String[] fields = lines[i].split(':')
          i++
          if (fields[0] == 'pub' || fields[0] == 'sub') {
            // start of new key
            throw new IllegalStateException(sprintf('Keygrip for key with id %s not found in GPG output:\n%s', [keyId, output]))
          }
          if (fields[0] == 'grp') {
            return fields[9]
          }
        }
      }
      throw new InvalidKeyException(sprintf('Key with id %s not found in GPG output:\n%s', [keyId, output]))
    }
  }

  private GpgUtils() {
    throw new UnsupportedOperationException()
  }
}
