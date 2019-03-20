#!/usr/bin/env groovy
/*
 * Unit tests for VersionUtils class
 * Copyright © 2017-2018  Basil Peace
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
package org.fidata.gradle.utils

import groovy.transform.CompileStatic
import org.junit.runner.RunWith
import org.junit.Test
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName

/**
 * Unit tests for {@link VersionUtils} class
 */
@RunWith(JUnitParamsRunner)
@CompileStatic
class VersionUtilsTest {
  /**
   * Test method for {@link VersionUtils#isPreReleaseVersion(java.lang.String)}.
   */
  @Test
  @Parameters
  @TestCaseName('{index}: isPreReleaseVersion({0}) == {1}')
  void testIsPreReleaseVersion(final String version, final Boolean expectedResult) {
    assert expectedResult == VersionUtils.isPreReleaseVersion(version)
  }

  static Object[] parametersForTestIsPreReleaseVersion() {
    [
      // SemVer
      [null                   , null],
      [''                     , null],
      ['1.0.0'                , Boolean.FALSE],
      ['1.0.0+20130313144700' , Boolean.FALSE],
      ['0.1.0'                , Boolean.FALSE], // We don't follow Magic Zero rule
      ['1.0.1-SNAPSHOT'       , Boolean.TRUE],
      ['1.0.0-alpha+001'      , Boolean.TRUE],
      ['1.0.0-alpha'          , Boolean.TRUE],
      // Java
      ['1.8.0_152'            , Boolean.FALSE],
      // org.spockframework:spock-core
      ['1.1-groovy-2.4'       , Boolean.FALSE],
      // javax.validation:validation-api
      ['2.0.0.Final'          , Boolean.FALSE],
      ['1.0.0.GA'             , Boolean.FALSE],
      ['2.0.0.Alpha2'         , Boolean.TRUE],
      ['2.0.0.CR1'            , Boolean.TRUE],
      // com.google.guava:guava
      ['23.5-jre'             , Boolean.FALSE],
      // Maven Version Order Specifications
      // See http://maven.apache.org/pom.html#Version_Order_Specification
      ['1-beta'               , Boolean.TRUE],
      ['1-milestone'          , Boolean.TRUE],
      ['1-rc'                 , Boolean.TRUE],
      ['1-cr'                 , Boolean.TRUE],
      ['1-snapshot'           , Boolean.TRUE],
      ['1'                    , Boolean.FALSE],
      ['1.final'              , Boolean.FALSE],
      ['1.ga'                 , Boolean.FALSE],
      ['1-ga.1'               , Boolean.FALSE],
      ['1-sp'                 , Boolean.FALSE],
      ['1-sp-1'               , Boolean.FALSE],
      ['1-a1'                 , Boolean.TRUE],
      ['1-alpha-1'            , Boolean.TRUE],
      ['1-b2'                 , Boolean.TRUE],
      ['1-m13'                , Boolean.TRUE],
      // The same, but with Semver versions
      ['1.0.0-final'          , Boolean.FALSE],
      ['1.0.0-ga'             , Boolean.FALSE],
      ['1.0.0-sp1'            , Boolean.FALSE],
      // Service Release
      ['1.0.0-SR-4'           , Boolean.FALSE],
      // Gradle
      // See https://github.com/gradle/gradle/commit/489524e3693b542abc4280a4e70a0e5467dd831c
      ['1.0.0-dev'            , Boolean.TRUE],
      ['1.0-dev'              , Boolean.TRUE],
      ['1.0.0-release'        , Boolean.FALSE],
      ['1.0-release'          , Boolean.FALSE],
      // Android
      ['HONEYCOMB_MR1'        , Boolean.FALSE],
    ]*.toArray().toArray()
  }
}
