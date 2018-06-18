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
package org.fidata.gradle.internal;

import lombok.Getter;
import lombok.AccessLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * Base class for extensions and conventions
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethod")
public abstract class AbstractExtension {
  // private @MonotonicNonNull PropertyChangeSupport propertyChangeSupport;

  /**
   * List of property change listeners
   */
  @Getter(/*value = AccessLevel.PROTECTED,*/ lazy = true)
  private final /*@MonotonicNonNull*/ PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);;
  /*protected @NonNull PropertyChangeSupport getPropertyChangeSupport() {
    if (propertyChangeSupport == null) {
      propertyChangeSupport = new PropertyChangeSupport(this);
    }
    return propertyChangeSupport;
  }*/

  /**
   * Adds a property change listener.
   * @param listener The {@link PropertyChangeListener} to be added.
   */
  public void addPropertyChangeListener(final @NonNull PropertyChangeListener listener) {
    getPropertyChangeSupport().addPropertyChangeListener(listener);
  }

  /**
   * Removes a property change listener.
   * @param listener The {@link PropertyChangeListener} to be removed
   */
  public void removePropertyChangeListener(final @NonNull PropertyChangeListener listener) {
    getPropertyChangeSupport().removePropertyChangeListener(listener);
  }
}
