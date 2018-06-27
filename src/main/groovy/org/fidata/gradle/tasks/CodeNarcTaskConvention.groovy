#!/usr/bin/env groovy
/*
 * CodeNarcTaskConvention class
 * Copyright © 2017-2018  Basil Peace
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
package org.fidata.gradle.tasks

import com.google.common.base.Charsets
import com.google.common.io.Resources
import groovy.text.StreamingTemplateEngine
import groovy.text.Template
import groovy.transform.CompileStatic
import org.fidata.gradle.internal.AbstractExtension
import org.gradle.api.plugins.quality.CodeNarc

/**
 * Convention to provide default configuraton for {@link org.gradle.api.plugins.quality.CodeNarc} tasks
 * Adds disabledRules property to disable specific rules per task
 */
@CompileStatic
class CodeNarcTaskConvention extends AbstractExtension {
  private static final String CODENARC_DEFAULT_CONFIG = Resources.toString(Resources.getResource(CodeNarcTaskConvention, 'config/codenarc/codenarc.groovy'), Charsets.UTF_8)

  private static final Template CODENARC_DISABLED_RULES_CONFIG_TEMPLATE = new StreamingTemplateEngine().createTemplate(Resources.toString(Resources.getResource(CodeNarcTaskConvention, 'config/codenarc/codenarc.disabledRules.groovy.template'), Charsets.UTF_8))

  /**
   * List of disabled rules
   */
  List<String> disabledRules = []

  CodeNarcTaskConvention(CodeNarc task) {
    super
    task.with {
      config = task.project.resources.text.fromString(CODENARC_DEFAULT_CONFIG)
    }
    task.doFirst {
      if (disabledRules?.size() > 0) {
        task.config = task.project.resources.text.fromString(
          task.config.asString() +
          CODENARC_DISABLED_RULES_CONFIG_TEMPLATE.make(disabledRules: disabledRules.inspect()).toString()
        )
      }
    }
  }
}
