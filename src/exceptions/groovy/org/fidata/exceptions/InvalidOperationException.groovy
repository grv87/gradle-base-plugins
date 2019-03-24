#!/usr/bin/env groovy
/*
 * InvalidOperationException class
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
package org.fidata.exceptions

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/*
 * WORKAROUND:
 * This should go to separate library
 * <grv87 2018-09-30>
 */
/**
 * The exception that is thrown when a method call is invalid for the object's current state.
 * Equivalent for .NET {@code System.InvalidOperationException}
 */
@CompileStatic
@InheritConstructors
class InvalidOperationException extends RuntimeException { }
