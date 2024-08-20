package uk.gov.laa.ccms.caab.model.summary;

import java.util.List;
import lombok.Data;

/**
 * Represents the opponents and other parties submission summary display.
 */
@Data
public class OpponentsAndOtherPartiesSubmissionSummaryDisplay {

  /**
   * The list of opponent submission summary displays.
   */
  private List<OpponentSubmissionSummaryDisplay> opponents;
}

