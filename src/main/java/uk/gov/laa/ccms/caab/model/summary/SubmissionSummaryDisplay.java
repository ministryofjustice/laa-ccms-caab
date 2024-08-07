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

@Data
@Builder
public class SubmissionSummaryDisplay {

  @Builder.Default
  private List<AssessmentSummaryEntityDisplay> meansAssessment =
      new ArrayList<>();

  @Builder.Default
  private List<AssessmentSummaryEntityDisplay> meritsAssessment =
      new ArrayList<>();

  @Builder.Default
  private ProviderSubmissionSummaryDisplay providerDetails =
      new ProviderSubmissionSummaryDisplay();

  @Builder.Default
  private ClientFlowFormData client =
      new ClientFlowFormData(ACTION_VIEW);

  @Builder.Default
  private HashMap<String, CommonLookupValueDetail> clientLookups =
      new HashMap<>();

  @Builder.Default
  private GeneralDetailsSubmissionSummaryDisplay generalDetails =
      new GeneralDetailsSubmissionSummaryDisplay();

  @Builder.Default
  private HashMap<String, CommonLookupValueDetail> generalDetailsLookups =
      new HashMap<>();

  @Builder.Default
  private ProceedingAndCostSubmissionSummaryDisplay proceedingsAndCosts =
      new ProceedingAndCostSubmissionSummaryDisplay();

  @Builder.Default
  private OpponentsAndOtherPartiesSubmissionSummaryDisplay opponentsAndOtherParties =
      new OpponentsAndOtherPartiesSubmissionSummaryDisplay();


}
