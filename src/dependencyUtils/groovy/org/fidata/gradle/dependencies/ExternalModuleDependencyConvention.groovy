/*
 * ExternalModuleDependencyConvention class
 * Copyright ©  Basil Peace
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
package org.fidata.gradle.dependencies

/**
 * Convention providing additional properties to ExternalModuleDependency
 */
class ExternalModuleDependencyConvention {
  /**
   * Desired dependency status.
   * Only default scheme
   * (see <a href="http://ant.apache.org/ivy/history/master/settings/statuses.html" target="_blank">Ivy reference documentation</a>)
   * is supported.
   * By default it is release
   */
  String status = 'release'
}
