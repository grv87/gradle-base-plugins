#!/usr/bin/env groovy
/*
 * CodeNarcTaskConvention class
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
  Set<String> disabledRules = []

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
