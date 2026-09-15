package uk.gov.laa.ccms.caab.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ControlCharacterStrippingEditorTest {

  /**
   * NUL, bell, escape and a C1 control character. Built from char codes rather than written into
   * the source, where they would be invisible to a reviewer.
   */
  private static final String CONTROL_CHARACTERS =
      String.valueOf(new char[] {0x00, 0x07, 0x1B, 0x9F});

  @Test
  void stripsControlCharacters() {
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor();

    editor.setAsText("Sm" + CONTROL_CHARACTERS + "ith");

    assertEquals("Smith", editor.getValue());
  }

  @Test
  void keepsWhitespaceThatTextareasNeed() {
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor();

    editor.setAsText("line one\r\nline two\tindented");

    assertEquals("line one\r\nline two\tindented", editor.getValue());
  }

  @Test
  void preservesEmptyStringByDefault() {
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor();

    editor.setAsText("");

    assertEquals("", editor.getValue());
  }

  @Test
  void preservesSurroundingWhitespaceByDefault() {
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor();

    editor.setAsText("  padded  ");

    assertEquals("  padded  ", editor.getValue());
  }

  @Test
  void handlesNull() {
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor();

    editor.setAsText(null);

    assertNull(editor.getValue());
  }

  @Test
  void trimmingVariantMatchesStringTrimmerEditorContract() {
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor(true);

    editor.setAsText("  padded  ");
    assertEquals("padded", editor.getValue());

    editor.setAsText("   ");
    assertNull(editor.getValue());

    editor.setAsText("");
    assertNull(editor.getValue());
  }

  @Test
  void trimmingVariantAlsoStripsControlCharacters() {
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor(true);

    editor.setAsText("  Sm" + CONTROL_CHARACTERS + "ith  ");

    assertEquals("Smith", editor.getValue());
  }

  @Test
  void doesNotAlterAngleBrackets() {
    // Angle brackets are a validation concern, reported to the user as a field error rather than
    // silently removed here.
    final ControlCharacterStrippingEditor editor = new ControlCharacterStrippingEditor();

    editor.setAsText("<script>");

    assertEquals("<script>", editor.getValue());
  }
}
