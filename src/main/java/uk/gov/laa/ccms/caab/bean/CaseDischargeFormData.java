package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/** Discharge answers use nullable booleans to distinguish No from unanswered. */
@Data
public class CaseDischargeFormData {
  private Boolean dischargeCaseInd;
  private String dischargeReason;
  private Boolean clientContinueInd;
  private String otherDetails;

  /** Matches legacy's textarea character counting. */
  public void normaliseLegacyFields() {
    if (otherDetails != null) {
      otherDetails = otherDetails.replace("\r\n", "\n");
    }
  }
}
