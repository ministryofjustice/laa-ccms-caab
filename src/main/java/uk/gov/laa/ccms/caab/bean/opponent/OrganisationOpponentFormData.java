package uk.gov.laa.ccms.caab.bean.opponent;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.DEFAULT_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.ORGANISATION_NAME_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
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
  @Size(max = ORGANISATION_NAME_CHARACTER_SIZE)
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
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String contactNameRole;

  /**
   * Flag to indicate this is a shared opponent.
   */
  private Boolean shared;
}
