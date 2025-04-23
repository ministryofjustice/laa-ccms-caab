package uk.gov.laa.ccms.caab.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DisplayUtilTest {

  @Test
  void getDelimitedStringBuildsCorrectly() {
    final List<String> items = List.of("1", "2", "3", "4");
    final String delimiter = "|";
    final String lastDelim = "*";

    final String result = DisplayUtil.getDelimitedString(items, delimiter, lastDelim);

    assertNotNull(result);
    assertEquals("%s%s%s%s%s%s%s".formatted(
        items.getFirst(), delimiter,
        items.get(1), delimiter,
        items.get(2), lastDelim,
        items.get(3)), result);
  }

  @Test
  void getDelimitedStringNoItemsBuildsCorrectly() {
    final List<String> items = Collections.emptyList();
    final String delimiter = "|";
    final String lastDelim = "*";

    final String result = DisplayUtil.getDelimitedString(items, delimiter, lastDelim);

    assertNotNull(result);
    assertEquals("", result);
  }

  @Test
  void getCommaDelimitedStringBuildsCorrectly() {
    final List<String> items = List.of("1", "2", "3", "4");
    final String delimiter = ", ";
    final String lastDelim = " or ";

    final String result = DisplayUtil.getCommaDelimitedString(items);

    assertNotNull(result);
    assertEquals("%s%s%s%s%s%s%s".formatted(
        items.getFirst(), delimiter,
        items.get(1), delimiter,
        items.get(2), lastDelim,
        items.get(3)), result);
  }
}
