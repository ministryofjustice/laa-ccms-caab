package uk.gov.laa.ccms.caab.bean.opponent;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;

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
  private String firstName;

  /**
   * THe middle name(s) of an individual opponent.
   */
  private String middleNames;

  /**
   * The surname of an individual opponent.
   */
  private String surname;

  /**
   * The date of birth of an individual opponent.
   */
  private String dateOfBirth;

  /**
   * The national insurance number of an individual opponent.
   */
  private String nationalInsuranceNumber;

  /**
   * The home telephone number for the opponent.
   */
  private String telephoneHome;

  /**
   * The mobile number for the opponent.
   */
  private String telephoneMobile;

  /**
   * Flag to indicate that an individual is receiving legal aid.
   */
  private Boolean legalAided;

  /**
   * The opponent's legal aid certificate number.
   */
  private String certificateNumber;

  /**
   * Flag to indicate that date of birth is mandatory for this opponent.
   */
  private boolean dateOfBirthMandatory;

}
