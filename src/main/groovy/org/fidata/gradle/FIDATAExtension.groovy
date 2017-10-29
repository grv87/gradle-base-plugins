#!/usr/bin/env groovy
/*
 * FIDATAExtension class
 * Copyright © 2017  Basil Peace
 *
 * This file is part of gradle-fidata-plugin.
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
package org.fidata.gradle

import groovy.transform.CompileStatic
import java.beans.PropertyChangeSupport
import java.beans.PropertyChangeListener

@CompileStatic
class FIDATAExtension {
  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this)

  /**Add a property change listener for a specific property.
  @param listener The <code>PropertyChangeListener</code>
      to be added.
  */
  public void addPropertyChangeListener(final PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener)
  }

  /**Remove a property change listener for a specific property.
  @param listener The <code>PropertyChangeListener</code>
      to be removed
  */
  public void removePropertyChangeListener(final PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener)
  }

  private boolean publicReleases = false

  boolean getPublicReleases() {
    this.publicReleases
  }

  void setPublicReleases(boolean newValue) {
    boolean oldValue = this.publicReleases
    this.publicReleases = newValue
    propertyChangeSupport.firePropertyChange('publicReleases', oldValue, newValue);
  }
}
