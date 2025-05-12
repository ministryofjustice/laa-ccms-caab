package uk.gov.laa.ccms.caab.model.summary;

import java.util.List;
import lombok.Data;

/**
 * Represents the proceeding submission summary display.
 */
@Data
public class ProceedingSubmissionSummaryDisplay {

  /**
   * The matter type.
   */
  private String matterType;

  /**
   * The proceeding.
   */
  private String proceeding;

  /**
   * The client involvement type.
   */
  private String clientInvolvementType;

  /**
   * The form of civil legal service.
   */
  private String formOfCivilLegalService;

  /**
   * The type of order.
   */
  private String typeOfOrder;

  /**
   * The list of scope limitation submission summary displays.
   */
  private List<ScopeLimitationSubmissionSummaryDisplay> scopeLimitations;
}
