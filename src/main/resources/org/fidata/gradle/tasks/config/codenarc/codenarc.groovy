#!/usr/bin/env groovy
/*
 * CodeNarc rules
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

ruleset {
  ruleset('rulesets/basic.xml')
  ruleset('rulesets/braces.xml')
  ruleset('rulesets/concurrency.xml')
  ruleset('rulesets/convention.xml') {
    TrailingComma(enabled: false)
  }
  ruleset('rulesets/design.xml') {
    NestedForLoop(enabled: false)
  }
  ruleset('rulesets/dry.xml') {
    DuplicateListLiteral(enabled: false)
    DuplicateMapLiteral(enabled: false)
    DuplicateStringLiteral(enabled: false)
  }
  ruleset('rulesets/exceptions.xml')
  ruleset('rulesets/formatting.xml') {
    Indentation(spacesPerIndentLevel: 2)
    LineLength(enabled: false)
    SpaceAroundMapEntryColon(enabled: false)
    SpaceAfterClosingBrace(enabled: false)
  }
  ruleset('rulesets/generic.xml')
  ruleset('rulesets/grails.xml')
  ruleset('rulesets/groovyism.xml')
  ruleset('rulesets/imports.xml')
  ruleset('rulesets/jdbc.xml')
  ruleset('rulesets/junit.xml')
  ruleset('rulesets/logging.xml')
  ruleset('rulesets/naming.xml')
  ruleset('rulesets/security.xml')
  ruleset('rulesets/serialization.xml')
  ruleset('rulesets/unnecessary.xml')
  ruleset('rulesets/unused.xml')
}
