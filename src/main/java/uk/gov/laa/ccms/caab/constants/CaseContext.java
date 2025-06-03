package uk.gov.laa.ccms.caab.constants;

/**
 * Enum representing different context paths for a case.
 *
 * @author Jamie Briggs
 */
public enum CaseContext {
  APPLICATION("application"),
  AMENDMENTS("amendments");

  private final String pathValue;

  CaseContext(String pathValue) {
    this.pathValue = pathValue;
  }

  public String getPathValue() {
    return pathValue;
  }

  /**
   * Determines if the current context represents an amendment.
   *
   * @return true if the current context is AMENDMENTS, otherwise false
   */
  public boolean isAmendment() {
    return this == AMENDMENTS;
  }

  /**
   * Determines if the current context represents an application.
   *
   * @return true if the current context is APPLICATION, otherwise false
   */
  public boolean isApplication() {
    return this == APPLICATION;
  }

  /**
   * Retrieves the CaseContext enum constant that corresponds to the given path value.
   *
   * @param pathValue the string representation of the path value to match against.
   * @return the matching CaseContext enum constant if a match is found.
   * @throws IllegalArgumentException if the provided path value does not correspond
   *     to any enum constant.
   */
  public static CaseContext fromPathValue(String pathValue) {
    for (CaseContext context : CaseContext.values()) {
      if (context.pathValue.equals(pathValue)) {
        return context;
      }
    }
    throw new IllegalArgumentException("Invalid path value: " + pathValue);
  }
}
