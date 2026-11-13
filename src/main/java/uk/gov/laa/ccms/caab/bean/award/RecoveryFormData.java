package uk.gov.laa.ccms.caab.bean.award;

import lombok.Data;

/** Editable inputs on the cost and financial award Recovery page. */
@Data
public class RecoveryFormData {
  private String solicitorRecoveryAmount = "0.00";
  private String solicitorRecoveryDate;
  private String solicitorAmountPaidToLsc = "0.00";
  private String courtRecoveryAmount = "0.00";
  private String courtRecoveryDate;
  private String courtAmountPaidToLsc = "0.00";
  private String clientRecoveryAmount = "0.00";
  private String clientRecoveryDate;
  private String clientAmountPaidToLsc = "0.00";
  private Boolean leaveOfCourtRequiredInd;
  private String offeredAmount = "0.00";
  private String offerDetails;

  /** Matches legacy date-field trimming and textarea CRLF-to-LF conversion. */
  public void normaliseLegacyFields() {
    solicitorRecoveryDate = normaliseReceiptDate(solicitorRecoveryDate);
    courtRecoveryDate = normaliseReceiptDate(courtRecoveryDate);
    clientRecoveryDate = normaliseReceiptDate(clientRecoveryDate);
    if (offerDetails != null) {
      offerDetails = offerDetails.replace("\r\n", "\n");
    }
  }

  private String normaliseReceiptDate(final String value) {
    if (value == null) {
      return null;
    }
    final String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
