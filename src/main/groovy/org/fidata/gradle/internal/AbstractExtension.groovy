#!/usr/bin/env groovy
/*
 * AbstractExtension class
 * Copyright © 2017  Basil Peace
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
package org.fidata.gradle.internal

import groovy.transform.CompileStatic
import javax.validation.constraints.NotNull
import java.beans.PropertyChangeSupport
import java.beans.PropertyChangeListener

/**
 * Base class for extensions and conventions
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractExtension {
  /**
   * List of property change listeners
   */
  protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this)

  /**
   * Adds a property change listener.
   * @param listener The {@link PropertyChangeListener} to be added.
   */
  void addPropertyChangeListener(@NotNull final PropertyChangeListener listener) {
    propertyChangeSupport.addPropertyChangeListener(listener)
  }

  /**
   * Removes a property change listener.
   * @param listener The {@link PropertyChangeListener} to be removed
   */
  void removePropertyChangeListener(@NotNull final PropertyChangeListener listener) {
    propertyChangeSupport.removePropertyChangeListener(listener)
  }
}
