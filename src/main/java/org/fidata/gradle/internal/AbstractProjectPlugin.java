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

import groovy.transform.Internal;
import lombok.AccessLevel;
import lombok.Getter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Base class for plugins applicable to @{link Project}.
 */
@Internal
public abstract class AbstractProjectPlugin implements Plugin<Project> {
  /**
   * Returns project which this plugin instance is applied to.
   *
   * @return target project
   */
  @Getter(value = AccessLevel.PROTECTED)
  private Project project;

  /**
   * Applies the plugin to the project.
   *
   * @param project The target project
   */
  public final void apply(@SuppressWarnings({"ParameterHidesMemberVariable", "checkstyle:hiddenfield"}) final Project project) {
    this.project = project;
    doApply();
  }

  /**
   * Applies the plugin to the project.
   *
   * <p>
   * This method should be overridden by implementations
   * </p>
   */
  protected abstract void doApply();
}
