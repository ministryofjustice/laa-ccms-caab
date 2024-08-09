package uk.gov.laa.ccms.caab.model.sections;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the Section display values for the application sections screen.
 */
@Data
@Builder
public class ApplicationSectionDisplay {

  private String caseReferenceNumber;

  @Builder.Default
  private ApplicationTypeSectionDisplay applicationType =
      new ApplicationTypeSectionDisplay();

  @Builder.Default
  private ProviderSectionDisplay provider =
      new ProviderSectionDisplay();

  @Builder.Default
  private GeneralDetailsSectionDisplay generalDetails =
      new GeneralDetailsSectionDisplay();

  @Builder.Default
  private ClientSectionDisplay client =
      new ClientSectionDisplay();

  @Builder.Default
  private ProceedingsAndCostsSectionDisplay proceedingsAndCosts =
      new ProceedingsAndCostsSectionDisplay();

  @Builder.Default
  private List<PriorAuthoritySectionDisplay> priorAuthorities =
      new ArrayList<>();

  @Builder.Default
  private OpponentsSectionDisplay opponentsAndOtherParties =
      new OpponentsSectionDisplay();

  @Builder.Default
  private ApplicationSectionStatusDisplay meansAssessment =
      new ApplicationSectionStatusDisplay();

  @Builder.Default
  private ApplicationSectionStatusDisplay meritsAssessment =
      new ApplicationSectionStatusDisplay();

  @Builder.Default
  private ApplicationSectionStatusDisplay documentUpload =
      new ApplicationSectionStatusDisplay();

}
