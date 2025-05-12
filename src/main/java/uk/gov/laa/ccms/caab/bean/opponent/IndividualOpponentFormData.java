package uk.gov.laa.ccms.caab.bean.opponent;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.DEFAULT_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.NATIONAL_INSURANCE_NUMBER_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.TELEPHONE_NUMBER_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.laa.ccms.caab.bean.common.Individual;

/**
 * Represents the individual opponent details stored during opponent creation/edit flows.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IndividualOpponentFormData extends AbstractOpponentFormData implements Individual {

  public IndividualOpponentFormData() {
    setType(OPPONENT_TYPE_INDIVIDUAL);
  }

  /**
   * The title of an individual opponent.
   */
  private String title;

  /**
   * The first name of an individual opponent.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String firstName;

  /**
   * THe middle name(s) of an individual opponent.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String middleNames;

  /**
   * The surname of an individual opponent.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String surname;

  /**
   * The date of birth of an individual opponent.
   */
  private String dateOfBirth;

  /**
   * The national insurance number of an individual opponent.
   */
  @Size(max = NATIONAL_INSURANCE_NUMBER_CHARACTER_SIZE)
  private String nationalInsuranceNumber;

  /**
   * The home telephone number for the opponent.
   */
  @Size(max = TELEPHONE_NUMBER_CHARACTER_SIZE)
  private String telephoneHome;

  /**
   * The mobile number for the opponent.
   */
  @Size(max = TELEPHONE_NUMBER_CHARACTER_SIZE)
  private String telephoneMobile;

  /**
   * Flag to indicate that an individual is receiving legal aid.
   */
  private Boolean legalAided;

  /**
   * The opponent's legal aid certificate number.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String certificateNumber;

  /**
   * Flag to indicate that date of birth is mandatory for this opponent.
   */
  private boolean dateOfBirthMandatory;

}
