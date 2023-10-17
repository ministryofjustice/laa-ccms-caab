package uk.gov.laa.ccms.caab.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the Summary display values for the application summary screen.
 */
@Data
@Builder
public class ApplicationSummaryDisplay {

  private String caseReferenceNumber;
  private String clientFullName;
  private String providerCaseReferenceNumber;

  @Builder.Default
  private ApplicationSummaryStatusDisplay applicationType =
      new ApplicationSummaryStatusDisplay();
  @Builder.Default
  private ApplicationSummaryStatusDisplay providerDetails =
      new ApplicationSummaryStatusDisplay();
  @Builder.Default
  private ApplicationSummaryStatusDisplay clientDetails =
      new ApplicationSummaryStatusDisplay();
  @Builder.Default
  private ApplicationSummaryStatusDisplay generalDetails =
      new ApplicationSummaryStatusDisplay();
  @Builder.Default
  private ApplicationSummaryStatusDisplay proceedingsAndCosts =
      new ApplicationSummaryStatusDisplay();
  @Builder.Default
  private ApplicationSummaryStatusDisplay opponentsAndOtherParties =
      new ApplicationSummaryStatusDisplay();

}