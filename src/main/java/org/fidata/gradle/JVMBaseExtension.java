/*
 * org.fidata.gradle.JVMBaseExtension class
 * Copyright © 2018  Basil Peace
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

import org.fidata.gradle.internal.AbstractExtension;
import lombok.Getter;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.internal.jvm.UnsupportedJavaRuntimeException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides JVM-specific properties
 */
public class JVMBaseExtension extends AbstractExtension {
  /**
   * @return Map with package names in keys and links to javadoc in values
   */
  @Getter
  private final Map<String, URI> javadocLinks = new HashMap<>();

  public JVMBaseExtension(Project project) {
    JavaVersion javaVersion = project.getConvention().getPlugin(JavaPluginConvention.class).getTargetCompatibility();
    URI javaseJavadoc;
    if (javaVersion.compareTo(JavaVersion.VERSION_1_5) >= 0 && javaVersion.compareTo(JavaVersion.VERSION_11) < 0) {
      javaseJavadoc = project.uri("https://docs.oracle.com/javase/" + javaVersion.getMajorVersion() + "/docs/api/index.html?");
    } else if (javaVersion.isJava11()) {
      javaseJavadoc = project.uri("https://download.java.net/java/early_access/jdk11/docs/api/");
    } else {
      throw new UnsupportedJavaRuntimeException(String.format("Unable to get javadoc URI for unsupported java version: %s", javaVersion));
    }
    javadocLinks.put("java", javaseJavadoc);
  }
}
