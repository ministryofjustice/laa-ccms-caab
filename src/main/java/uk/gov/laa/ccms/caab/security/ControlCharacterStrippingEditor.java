package uk.gov.laa.ccms.caab.security;

import java.beans.PropertyEditorSupport;
import java.util.regex.Pattern;

/**
 * Removes control characters from every bound String.
 *
 * <p>Registered globally by {@link uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice}. Unlike a {@code
 * Validator} added through {@code WebDataBinder.addValidators}, a property editor runs during
 * binding itself, so this applies to every request handler regardless of whether the handler opts
 * in with {@code @Validated}.
 *
 * <p>Control characters are stripped rather than rejected. They carry no meaning a provider could
 * have intended, are invisible in an error message, and are never a legitimate part of a name,
 * reference or free-text note - so silently discarding them is preferable to failing a submission
 * over something the user cannot see. Tab, carriage return and line feed are kept, because
 * textareas legitimately contain them.
 *
 * <p>Empty strings are deliberately preserved rather than converted to null, so that the
 * empty-versus-absent distinction the existing validators rely on is unchanged.
 */
public class ControlCharacterStrippingEditor extends PropertyEditorSupport {

  /** C0 and C1 control characters, excluding tab, carriage return and line feed. */
  private static final Pattern CONTROL_CHARACTERS =
      Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\u009F]");

  private final boolean trimAndTreatEmptyAsNull;

  /** Strips control characters, leaving surrounding whitespace and empty strings untouched. */
  public ControlCharacterStrippingEditor() {
    this(false);
  }

  /**
   * Strips control characters, optionally also trimming and nullifying empty values.
   *
   * @param trimAndTreatEmptyAsNull whether to trim the value and convert an empty result to null,
   *     matching the contract of Spring's {@code StringTrimmerEditor(true)}.
   */
  public ControlCharacterStrippingEditor(final boolean trimAndTreatEmptyAsNull) {
    this.trimAndTreatEmptyAsNull = trimAndTreatEmptyAsNull;
  }

  /**
   * Strips control characters from the submitted text.
   *
   * @param text the raw bound value.
   */
  @Override
  public void setAsText(final String text) {
    if (text == null) {
      setValue(null);
      return;
    }

    String cleaned = CONTROL_CHARACTERS.matcher(text).replaceAll("");

    if (trimAndTreatEmptyAsNull) {
      cleaned = cleaned.trim();
      if (cleaned.isEmpty()) {
        setValue(null);
        return;
      }
    }

    setValue(cleaned);
  }
}
