/*
 * AbstractExtension class
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Base class for extensions and conventions.
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethod")
public abstract class AbstractExtension {
  /**
   * Returns list of property change listeners.
   *
   * @return list of property change listeners
   */
  @Getter(value = AccessLevel.PROTECTED, lazy = true)
  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * Adds a property change listener.
   *
   * @param listener The {@link PropertyChangeListener} to be added.
   */
  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    getPropertyChangeSupport().addPropertyChangeListener(listener);
  }

  /**
   * Removes a property change listener.
   *
   * @param listener The {@link PropertyChangeListener} to be removed
   */
  public void removePropertyChangeListener(final PropertyChangeListener listener) {
    getPropertyChangeSupport().removePropertyChangeListener(listener);
  }
}
