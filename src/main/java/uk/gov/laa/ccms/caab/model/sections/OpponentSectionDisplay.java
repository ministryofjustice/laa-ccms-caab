package uk.gov.laa.ccms.caab.model.sections;

import lombok.Builder;
import lombok.Data;

/** Represents the Section display data for an opponent. */
@Data
@Builder
public class OpponentSectionDisplay {
  /** The id of the opponent. */
  private Integer id;

  /** The EBS id of the opponent. */
  private String ebsId;

  /** This opponent's name (individual or organisation name). */
  private String partyName;

  /** The type of opponent. */
  private String partyType;

  /** The opponents relationship to the case. */
  private String relationshipToCase;

  /** The opponents relationship to the client. */
  private String relationshipToClient;

  /** Flag to indicate that the opponent is editable. */
  private boolean editable;
}
