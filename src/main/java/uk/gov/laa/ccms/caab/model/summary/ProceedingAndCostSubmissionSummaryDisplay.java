package uk.gov.laa.ccms.caab.model.summary;

import java.util.List;
import lombok.Data;

/** Represents the proceeding and cost submission summary display. */
@Data
public class ProceedingAndCostSubmissionSummaryDisplay {

  /** The list of proceeding submission summary displays. */
  private List<ProceedingSubmissionSummaryDisplay> proceedings;

  /** The case cost limitation. */
  private String caseCostLimitation;
}
