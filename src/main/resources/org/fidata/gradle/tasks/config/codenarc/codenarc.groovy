#!/usr/bin/env groovy
/*
 * CodeNarc rules
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
package org.fidata.gradle.tasks.config.codenarc

ruleset {
  ruleset('rulesets/basic.xml')
  ruleset('rulesets/braces.xml')
  ruleset('rulesets/comments.xml')
  ruleset('rulesets/concurrency.xml')
  ruleset('rulesets/convention.xml') {
    PublicMethodsBeforeNonPublicMethods(enabled: Boolean.FALSE)
    StaticFieldsBeforeInstanceFields(enabled: Boolean.FALSE)
    StaticMethodsBeforeInstanceMethods(enabled: Boolean.FALSE)
    TrailingComma(enabled: Boolean.FALSE)
  }
  ruleset('rulesets/design.xml') {
    NestedForLoop(enabled: Boolean.FALSE)
  }
  ruleset('rulesets/dry.xml') {
    DuplicateListLiteral(enabled: Boolean.FALSE)
    DuplicateMapLiteral(enabled: Boolean.FALSE)
    DuplicateStringLiteral(enabled: Boolean.FALSE)
  }
  ruleset('rulesets/exceptions.xml')
  ruleset('rulesets/formatting.xml') {
    ClassStartsWithBlankLine(blankLineRequired: Boolean.FALSE)
    ClassEndsWithBlankLine(blankLineRequired: Boolean.FALSE)
    Indentation(spacesPerIndentLevel: 2)
    LineLength(enabled: false)
    SpaceAroundMapEntryColon(enabled: Boolean.FALSE)
  }
  ruleset('rulesets/generic.xml')
  ruleset('rulesets/grails.xml')
  ruleset('rulesets/groovyism.xml')
  ruleset('rulesets/imports.xml')
  ruleset('rulesets/jdbc.xml')
  ruleset('rulesets/junit.xml')
  ruleset('rulesets/logging.xml')
  ruleset('rulesets/naming.xml')
  ruleset('rulesets/security.xml') {
    JavaIoPackageAccess(enabled: Boolean.FALSE)
  }
  ruleset('rulesets/serialization.xml')
  ruleset('rulesets/unnecessary.xml')
  ruleset('rulesets/unused.xml')
}
