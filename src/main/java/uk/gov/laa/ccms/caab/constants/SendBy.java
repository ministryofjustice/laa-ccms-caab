package uk.gov.laa.ccms.caab.constants;

import java.util.Arrays;
import lombok.Getter;

/**
 * Enumeration to describe the methods of sending a notification attachment.
 */
@Getter
public enum SendBy {

  ELECTRONIC("E", "Electronic"),
  POSTAL("P", "Postal");

  private final String code;

  private final String description;

  SendBy(final String code, final String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Create a new instance of SendBy from a string representation of the code, e.g. "E".
   *
   * @param rawCode the string representation of a code.
   * @return a SendBy instance corresponding to the provided code, if valid.
   * @throws IllegalArgumentException when the provided code does not correspond to an enum value.
   */
  public static SendBy fromCode(String rawCode) {
    return Arrays.stream(SendBy.values())
        .filter(sendBy -> sendBy.getCode().equals(rawCode))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("%s is not a valid SendBy code", rawCode)));
  }

}
