package uk.gov.laa.ccms.caab.model.summary;

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
public class GeneralDetailsSummaryDisplay extends ApplicationSummaryStatusDisplay {

  /**
   * The status of the application.
   */
  private String applicationStatus;

  /**
   * The category of law for the application.
   */
  private String categoryOfLaw;

  /**
   * The preferred correspondence method.
   */
  private String correspondenceMethod;

}
