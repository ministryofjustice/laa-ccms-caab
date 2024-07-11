package uk.gov.laa.ccms.caab.model.summary;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the Summary display values for the application summary screen.
 */
@Data
@Builder
public class ApplicationSummaryDisplay {

  private String caseReferenceNumber;

  @Builder.Default
  private ApplicationTypeSummaryDisplay applicationType =
      new ApplicationTypeSummaryDisplay();

  @Builder.Default
  private ProviderSummaryDisplay provider =
      new ProviderSummaryDisplay();

  @Builder.Default
  private GeneralDetailsSummaryDisplay generalDetails =
      new GeneralDetailsSummaryDisplay();

  @Builder.Default
  private ClientSummaryDisplay client =
      new ClientSummaryDisplay();

  @Builder.Default
  private ProceedingsAndCostsSummaryDisplay proceedingsAndCosts =
      new ProceedingsAndCostsSummaryDisplay();

  @Builder.Default
  private List<PriorAuthoritySummaryDisplay> priorAuthorities =
      new ArrayList<>();

  @Builder.Default
  private OpponentsSummaryDisplay opponentsAndOtherParties =
      new OpponentsSummaryDisplay();

  @Builder.Default
  private ApplicationSummaryStatusDisplay meansAssessment =
      new ApplicationSummaryStatusDisplay();

  @Builder.Default
  private ApplicationSummaryStatusDisplay meritsAssessment =
      new ApplicationSummaryStatusDisplay();

  @Builder.Default
  private ApplicationSummaryStatusDisplay documentUpload =
      new ApplicationSummaryStatusDisplay();

}
