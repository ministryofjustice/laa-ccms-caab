package uk.gov.laa.ccms.caab.constants;

import lombok.Getter;

/** Enumeration to describe the possible values for CCMS Module. */
@Getter
public enum CcmsModule {
  APPLICATION("A", "Create Application"),
  AMENDMENT("M", "Application Amendment"),
  OUTCOME("O", "Record Outcome"),
  REQUEST("R", "Provider Request");

  private final String code;

  private final String description;

  CcmsModule(final String code, final String description) {
    this.code = code;
    this.description = description;
  }
}
