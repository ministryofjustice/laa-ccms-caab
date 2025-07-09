package uk.gov.laa.ccms.caab.model.summary;

import lombok.Data;

/** Represents the provider submission summary display. */
@Data
public class ProviderSubmissionSummaryDisplay {

  /** The provider's office. */
  private String office;

  /** The Name of solicitor or Fellow of the Institute of Legal Executives instructed. */
  private String feeEarner;

  /** The provider's supervisor name. */
  private String supervisor;

  /** The provider case reference. */
  private String providerCaseReference;

  /** The provider contact name. */
  private String contactName;
}
