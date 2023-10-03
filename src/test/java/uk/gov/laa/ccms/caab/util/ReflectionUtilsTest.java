package uk.gov.laa.ccms.caab.util;

import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilsTest {

  static class TestClassNoFields {
  }

  static class TestClassNonStringFields {
    int number = 42;
    double value = 42.5;
  }

  static class TestClassWithStrings {
    String string1 = "value1";
    String string2 = "   ";
    String string3 = "";
    int number = 42;
  }

  static class TestClassWithPrivateFinalField {
    private final String privateFinalString = "someValue";
  }

  @Test
  void testNullifyStrings_NoFields() {
    TestClassNoFields obj = new TestClassNoFields();
    ReflectionUtils.nullifyStrings(obj);
    // Just asserting that no exception is thrown
    assertNotNull(obj);
  }

  @Test
  void testNullifyStrings_NonStringFields() {
    TestClassNonStringFields obj = new TestClassNonStringFields();
    ReflectionUtils.nullifyStrings(obj);
    assertEquals(42, obj.number);
    assertEquals(42.5, obj.value, 0.01);
  }

  @Test
  void testNullifyStrings_StringFieldsWithValues() {
    TestClassWithStrings obj = new TestClassWithStrings();
    ReflectionUtils.nullifyStrings(obj);
    assertEquals("value1", obj.string1);
    assertNull(obj.string2);
    assertNull(obj.string3);
    assertEquals(42, obj.number);
  }
}
