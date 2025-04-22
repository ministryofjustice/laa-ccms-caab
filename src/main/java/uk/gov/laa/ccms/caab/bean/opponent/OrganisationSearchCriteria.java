package uk.gov.laa.ccms.caab.bean.opponent;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.DEFAULT_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.ORGANISATION_NAME_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.POSTCODE_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

/**
 * Represents the criteria to search for an Organisation opponent.
 */
@Data
public class OrganisationSearchCriteria implements Serializable {

  /**
   * The name of the organisation.
   */
  @Size(max = ORGANISATION_NAME_CHARACTER_SIZE)
  private String name;

  /**
   * The type of the organisation.
   */
  private String type;

  /**
   * The organisation city.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String city;

  /**
   * The organisation postcode.
   */
  @Size(max = POSTCODE_CHARACTER_SIZE)
  private String postcode;

}

