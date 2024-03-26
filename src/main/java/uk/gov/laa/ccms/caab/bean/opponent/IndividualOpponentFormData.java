package uk.gov.laa.ccms.caab.bean.opponent;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;

import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Represents the individual opponent details stored during opponent creation/edit flows.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IndividualOpponentFormData extends AbstractOpponentFormData {

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
   * The day of birth of an individual opponent.
   */
  private String dobDay;

  /**
   * The month of birth of an individual opponent.
   */
  private String dobMonth;

  /**
   * The year of birth of an individual opponent.
   */
  private String dobYear;


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

  /**
   * Retrieves the formatted date of birth based on the day, month, and year values.
   *
   * @return The formatted date of birth (yyyy-MM-dd), or null if the date components are not valid
   *         integers.
   */
  public LocalDate getDateOfBirth() {
    LocalDate dateOfBirth = null;

    if (StringUtils.hasText(dobYear)
        && StringUtils.hasText(dobMonth)
        && StringUtils.hasText(dobDay)) {
      try {
        int year = Integer.parseInt(dobYear);
        int month = Integer.parseInt(dobMonth);
        int day = Integer.parseInt(dobDay);

        dateOfBirth = LocalDate.of(year, month, day);
      } catch (NumberFormatException e) {
        // Handle the exception if any of the dobYear, dobMonth, or dobDay is not a valid integer
        throw new CaabApplicationException("Unable to format date of birth", e);
      }
    }

    return dateOfBirth;
  }
}
