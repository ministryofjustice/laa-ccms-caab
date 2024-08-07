package uk.gov.laa.ccms.caab.model.summary;

import java.util.List;
import lombok.Data;

@Data
public class ProceedingAndCostSubmissionSummaryDisplay {

  private List<ProceedingSubmissionSummaryDisplay> proceedings;

  private String caseCostLimitation;

}
