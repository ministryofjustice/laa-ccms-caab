package uk.gov.laa.ccms.caab.security;

import java.beans.PropertyEditorSupport;
import java.util.regex.Pattern;

/**
 * Applies the service-wide input baseline to every bound String.
 *
 * <p>Registered by {@link uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice}. A property editor runs
 * during binding, so this reaches every handler without the model attribute needing
 * {@code @Validated}, and it only ever sees values the request actually submitted. That matters
 * because most form beans are carried across several pages: a validator over the whole bean would
 * reject a value set on an earlier page and render the error on a page that does not show the
 * field.
 *
 * <p>Control characters are stripped rather than rejected. They carry no meaning a provider could
 * have intended and are invisible in an error message, so discarding them beats failing a
 * submission over something the user cannot see. Tab, carriage return and line feed are kept,
 * because textareas legitimately contain them.
 *
 * <p>Angle brackets are rejected, because no legitimate CCMS value needs them and silently removing
 * characters a provider typed would be worse than telling them. The resulting message comes from
 * the {@code typeMismatch.java.lang.String} key.
 *
 * <p>Empty strings are deliberately preserved rather than converted to null, so that the
 * empty-versus-absent distinction the existing validators rely on is unchanged.
 */
public class ControlCharacterStrippingEditor extends PropertyEditorSupport {

  /** C0 and C1 control characters, excluding tab, carriage return and line feed. */
  private static final Pattern CONTROL_CHARACTERS =
      Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\u009F]");

  private static final Pattern ANGLE_BRACKETS = Pattern.compile("[<>]");

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
   * Applies the baseline to the submitted text.
   *
   * @param text the raw bound value.
   * @throws IllegalArgumentException when the value contains an angle bracket.
   */
  @Override
  public void setAsText(final String text) {
    if (text == null) {
      setValue(null);
      return;
    }

    if (ANGLE_BRACKETS.matcher(text).find()) {
      throw new IllegalArgumentException("Value contains an invalid character");
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
