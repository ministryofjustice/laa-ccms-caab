package uk.gov.laa.ccms.caab.model.summary;

import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_VIEW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.model.assessment.AssessmentSummaryEntityDisplay;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/** Represents the submission summary display containing various assessment and detail displays. */
@Data
@Builder
public class SubmissionSummaryDisplay {

  /** List of means assessment summary entity displays. */
  @Builder.Default private List<AssessmentSummaryEntityDisplay> meansAssessment = new ArrayList<>();

  /** List of merits assessment summary entity displays. */
  @Builder.Default
  private List<AssessmentSummaryEntityDisplay> meritsAssessment = new ArrayList<>();

  /** Provider submission summary display. */
  @Builder.Default
  private ProviderSubmissionSummaryDisplay providerDetails = new ProviderSubmissionSummaryDisplay();

  /** Client flow form data. */
  @Builder.Default private ClientFlowFormData client = new ClientFlowFormData(ACTION_VIEW);

  /** HashMap of client lookup values. */
  @Builder.Default private HashMap<String, CommonLookupValueDetail> clientLookups = new HashMap<>();

  /** General details submission summary display. */
  @Builder.Default
  private GeneralDetailsSubmissionSummaryDisplay generalDetails =
      new GeneralDetailsSubmissionSummaryDisplay();

  /** Proceeding and cost submission summary display. */
  @Builder.Default
  private ProceedingAndCostSubmissionSummaryDisplay proceedingsAndCosts =
      new ProceedingAndCostSubmissionSummaryDisplay();

  /** Opponents and other parties submission summary display. */
  @Builder.Default
  private OpponentsAndOtherPartiesSubmissionSummaryDisplay opponentsAndOtherParties =
      new OpponentsAndOtherPartiesSubmissionSummaryDisplay();
}
