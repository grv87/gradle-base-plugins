/*
 * JDKExtension class
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
package org.fidata.gradle;

import org.fidata.gradle.internal.AbstractExtension;
import lombok.Getter;

/**
 * Provides JDK choice
 */
public class JDKExtension extends AbstractExtension {
  /**
   * Source JDK version
   */
  @Getter
  private String sourceVersion;

  /**
   * Sets source JDK version
   */
  void setSourceVersion(String newValue) {
    String oldValue = sourceVersion;
    sourceVersion = newValue;
    propertyChangeSupport.firePropertyChange("sourceVersion", oldValue, newValue);
  }

  /**
   * Target JDK version
   */
  @Getter
  private String targetVersion;

  /**
   * Sets target JDK version
   */
  void setTargetVersion(String newValue) {
    String oldValue = targetVersion;
    targetVersion = newValue;
    propertyChangeSupport.firePropertyChange("targetVersion", oldValue, newValue);
  }

  /**
   * Sets both source and target JDK version to the same value
   */
  void setVersion(String newValue) {
    String oldSourceValue = sourceVersion;
    String oldTargetValue = targetVersion;
    sourceVersion = newValue;
    targetVersion = newValue;
    propertyChangeSupport.firePropertyChange("sourceVersion", oldSourceValue, newValue);
    propertyChangeSupport.firePropertyChange("targetVersion", oldTargetValue, newValue);
  }
}
