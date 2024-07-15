package uk.gov.laa.ccms.caab.model.summary;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the display data for an opponent.
 */
@Data
@Builder
public class OpponentSummaryDisplay {
  /** This opponent's name (individual or organisation name). */
  private String partyName;

  /** The type of opponent. */
  private String partyType;

  /** The opponents relationship to the case. */
  private String relationshipToCase;

  /** The opponents relationship to the client. */
  private String relationshipToClient;

}
