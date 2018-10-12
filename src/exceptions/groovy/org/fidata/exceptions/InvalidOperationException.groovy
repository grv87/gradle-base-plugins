#!/usr/bin/env groovy
/*
 * InvalidOperationException class
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
