/*
 * org.fidata.gradle.JVMBaseExtension class
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
package org.fidata.gradle;

import org.fidata.gradle.internal.AbstractExtension;
import lombok.Getter;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
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
    javadocLinks.put("java", project.uri("https://docs.oracle.com/javase/" + project.getConvention().getPlugin(JavaPluginConvention.class).getTargetCompatibility().getMajorVersion() + "/docs/api/"));
  }
}
