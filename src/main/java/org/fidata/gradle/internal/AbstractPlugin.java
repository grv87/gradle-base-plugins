/*
 * AbstractPlugin class
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
package org.fidata.gradle.internal;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Base class for plugins
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethod")
public abstract class AbstractPlugin implements Plugin<Project> {
  private Project project;

  /**
   * Gets project which this plugin instance is applied to
   */
  protected final Project getProject() {
    return project;
  }

  /**
   * Applies the plugin to the project
   */
  public void apply(Project project) {
    this.project = project;
  }

}
