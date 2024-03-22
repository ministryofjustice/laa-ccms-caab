package uk.gov.laa.ccms.caab.bean.opponent;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the organisation opponent details stored during opponent creation/edit flows.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganisationOpponentFormData extends AbstractOpponentFormData {

  public OrganisationOpponentFormData() {
    setType(OPPONENT_TYPE_ORGANISATION);
  }

  /**
   * The name of the organisation Opponent.
   */
  private String organisationName;

  /**
   * The type of organisation).
   */
  private String organisationType;

  /**
   * The display value for the type of organisation.
   */
  private String organisationTypeDisplayValue;

  /**
   * Flag to indicate that an organisation is currently trading.
   */
  private Boolean currentlyTrading;

  /**
   * The contact name and role.
   */
  private String contactNameRole;

  /**
   * Flag to indicate this is a shared opponent.
   */
  private Boolean shared;
}
