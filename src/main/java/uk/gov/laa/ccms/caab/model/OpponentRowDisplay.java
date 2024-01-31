package uk.gov.laa.ccms.caab.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the display data for an opponent.
 */
@Data
@Builder
public class OpponentRowDisplay {
  /** Unique identifier for the opponent. */
  private Integer id;

  /** This opponent's name (individual or organisation name). */
  private String partyName;

  /** The type of opponent. */
  private String partyType;

  /** The opponents relationship to the case. */
  private String relationshipToCase;

  /** The opponents relationship to the client. */
  private String relationshipToClient;

  /** Whether this opponent can be edited. */
  private boolean editable;

  /** Whether this opponent can be removed. */
  private boolean deletable;
}
