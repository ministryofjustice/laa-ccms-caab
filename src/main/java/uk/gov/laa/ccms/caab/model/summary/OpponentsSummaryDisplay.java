package uk.gov.laa.ccms.caab.model.summary;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the Summary display values for the application summary screen.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OpponentsSummaryDisplay extends ApplicationSummaryStatusDisplay {

  /**
   * The list of opponents for the application.
   */
  private List<OpponentSummaryDisplay> opponents;

}
