/*
 * PathDirector interface
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
package org.fidata.gradle.utils;

import org.gradle.api.Namer;
import org.gradle.internal.FileUtils;

import java.nio.file.Path;

/**
 * A path director is capable of providing a path based on some inherent characteristics of an object.
 * Analogue for {@link Namer}
 */
public interface PathDirector<T> {
  /**
   * Determines the path for the given object.
   * Implementation should manually call {@link FileUtils#toSafeFileName}
   * on individual directory/file names whenever necessary
   *
   * @param object The object to determine the path for
   * @return The path for the object. Never null
   * @throws RuntimeException If the path cannot be determined or is null
   */
  Path determinePath(T object) throws RuntimeException;
}
