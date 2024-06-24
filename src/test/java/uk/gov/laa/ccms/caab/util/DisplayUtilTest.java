package uk.gov.laa.ccms.caab.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DisplayUtilTest {

  @Test
  void testGetDelimitedString_buildsCorrectly() {
    final List<String> items = List.of("1", "2", "3", "4");
    final String delimiter = "|";
    final String lastDelim = "*";

    final String result = DisplayUtil.getDelimitedString(items, delimiter, lastDelim);

    assertNotNull(result);
    assertEquals(String.format("%s%s%s%s%s%s%s",
        items.get(0), delimiter,
        items.get(1), delimiter,
        items.get(2), lastDelim,
        items.get(3)), result);
  }

  @Test
  void testGetDelimitedString_noItems_buildsCorrectly() {
    final List<String> items = Collections.emptyList();
    final String delimiter = "|";
    final String lastDelim = "*";

    final String result = DisplayUtil.getDelimitedString(items, delimiter, lastDelim);

    assertNotNull(result);
    assertEquals("", result);
  }

  @Test
  void testGetCommaDelimitedString_buildsCorrectly() {
    final List<String> items = List.of("1", "2", "3", "4");
    final String delimiter = ", ";
    final String lastDelim = " or ";

    final String result = DisplayUtil.getCommaDelimitedString(items);

    assertNotNull(result);
    assertEquals(String.format("%s%s%s%s%s%s%s",
        items.get(0), delimiter,
        items.get(1), delimiter,
        items.get(2), lastDelim,
        items.get(3)), result);
  }
}