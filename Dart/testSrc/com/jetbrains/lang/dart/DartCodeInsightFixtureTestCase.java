package com.jetbrains.lang.dart;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.jetbrains.lang.dart.util.DartTestUtils;

abstract public class DartCodeInsightFixtureTestCase extends LightPlatformCodeInsightFixtureTestCase {
  protected void setUp() throws Exception {
    super.setUp();
    DartTestUtils.configureDartSdk(myModule);
  }

  @Override
  protected String getTestDataPath() {
    return DartTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }
}
