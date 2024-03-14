package uk.gov.laa.ccms.caab.bean;

import java.time.LocalDate;
import lombok.Data;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Represents the opponent details stored during opponent creation/edit flows.
 */
@Data
public class OpponentFormData {

  /**
   * The id of the opponent.
   */
  private Integer id;

  /**
   * The partyId of the opponent.
   */
  private String partyId;

  /**
   * The party type of the Opponent.
   */
  private String type;

  /**
   * The party name, which depends on the type of Opponent (Organisation or Individual).
   */
  private String partyName;

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
   * The name of the organisation Opponent (for opponents of type ORGANISATION only).
   */
  private String organisationName;

  /**
   * The type of organisation (for opponents of type ORGANISATION only).
   */
  private String organisationType;

  /**
   * The display value for the type of organisation (for opponents of type ORGANISATION only).
   */
  private String organisationTypeDisplayValue;

  /**
   * Flag to indicate that an organisation is currently trading.
   */
  private Boolean currentlyTrading;

  /**
   * The opponent's relationship to the case.
   */
  private String relationshipToCase;

  /**
   * The display value for the opponent's relationship to the case.
   */
  private String relationshipToCaseDisplayValue;

  /**
   * The opponent's relationship to the client.
   */
  private String relationshipToClient;

  /**
   * The display value for the opponent's relationship to the client.
   */
  private String relationshipToClientDisplayValue;

  /**
   * The contact name and role.
   */
  private String contactNameRole;

  /**
   * The opponent house name/number.
   */
  private String houseNameOrNumber;

  /**
   * The address line 1 for the opponent.
   */
  private String addressLine1;

  /**
   * The address line 2 for the opponent.
   */
  private String addressLine2;

  /**
   * The city for the opponent.
   */
  private String city;

  /**
   * The opponent's county.
   */
  private String county;

  /**
   * The opponent's country.
   */
  private String country;

  /**
   * The postcode for the opponent.
   */
  private String postcode;

  /**
   * The home telephone number for the opponent.
   */
  private String telephoneHome;

  /**
   * The work telephone number for the opponent.
   */
  private String telephoneWork;

  /**
   * The mobile number for the opponent.
   */
  private String telephoneMobile;

  /**
   * The fax number for the opponent.
   */
  private String faxNumber;

  /**
   * The email address for the opponent.
   */
  private String emailAddress;

  /**
   * Any other information regarding the opponent.
   */
  private String otherInformation;

  /**
   * Flag to indicate that an individual is receiving legal aid.
   */
  private Boolean legalAided;

  /**
   * The opponent's legal aid certificate number.
   */
  private String certificateNumber;

  /**
   * Flag to indicate this is a shared opponent.
   */
  private Boolean shared;

  /**
   * Flag to indicate that this opponent can be modified.
   */
  private Boolean editable;

  /**
   * Flag to indicate that this opponent can be deleted.
   */
  private Boolean deletable;

  /**
   * The app mode flag.
   */
  private Boolean appMode;

  /**
   * The amendment flag.
   */
  private Boolean amendment;

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
    try {
      int year = Integer.parseInt(dobYear);
      int month = Integer.parseInt(dobMonth);
      int day = Integer.parseInt(dobDay);

      return LocalDate.of(year, month, day);
    } catch (NumberFormatException e) {
      // Handle the exception if any of the dobYear, dobMonth, or dobDay is not a valid integer
      throw new CaabApplicationException("Unable to format date of birth", e);
    }
  }

}
