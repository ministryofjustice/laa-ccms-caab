package uk.gov.laa.ccms.caab.model.sections;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the Section display data for an opponent.
 */
@Data
@Builder
public class OpponentSectionDisplay {
  /** This opponent's name (individual or organisation name). */
  private String partyName;

  /** The type of opponent. */
  private String partyType;

  /** The opponents relationship to the case. */
  private String relationshipToCase;

  /** The opponents relationship to the client. */
  private String relationshipToClient;

}
