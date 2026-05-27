package uk.gov.laa.ccms.caab.constants;

/** Enumeration of FTS fields that should be DAT / have date inputs. */
public enum ProviderRequestDateFields {
  PCASEBALS3("PCASEBALS3"),
  CCASEBALS3("CCASEBALS3");

  private final String code;

  ProviderRequestDateFields(final String code) {
    this.code = code;
  }

  public static boolean isDateField(String code) {
    if (code == null) {
      return false;
    }

    for (ProviderRequestDateFields field : values()) {
      if (field.code.equals(code)) {
        return true;
      }
    }
    return false;
  }
}
