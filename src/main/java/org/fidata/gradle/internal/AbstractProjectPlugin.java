/*
 * AbstractProjectPlugin class
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
package org.fidata.gradle.internal;

import lombok.AccessLevel;
import lombok.Getter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Base class for plugins applicable to @{link Project}
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethod")
public abstract class AbstractProjectPlugin implements Plugin<Project> {
  /**
   * @return project which this plugin instance is applied to
   */
  @Getter(value = AccessLevel.PROTECTED)
  private Project project;

  /**
   * Applies the plugin to the project
   */
  public void apply(Project project) {
    this.project = project;
  }
}
