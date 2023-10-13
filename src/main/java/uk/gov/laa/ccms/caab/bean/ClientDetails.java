package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Represents the details of a client.
 */
@Data
public class ClientDetails {

  //basic details
  private String title;
  private String surname;
  private String firstName;
  private String middleNames;
  private String surnameAtBirth;
  private String dobDay;
  private String dobMonth;
  private String dobYear;
  private String countryOfOrigin;
  private String nationalInsuranceNumber;
  private String homeOfficeNumber;
  private String gender;
  private String maritalStatus;
  private Boolean vulnerableClient = false;
  private Boolean highProfileClient = false;
  private Boolean vexatiousLitigant = false;
  private Boolean mentalIncapacity = false;

  //contact details
  private String telephoneHome;
  private String telephoneWork;
  private String telephoneMobile;
  private String emailAddress;
  private String password;
  private String passwordReminder;
  private String correspondenceMethod;
  private String correspondenceLanguage;

  //address details
  private Boolean noFixedAbode = false;
  private String country;
  private String houseNameNumber;
  private String postcode;
  private String addressLine1;
  private String addressLine2;
  private String cityTown;
  private String county;

  //address search details
  private String uprn;
  private boolean noAddressLookup = false;

  //equal opportunities monitoring
  private String ethnicOrigin;
  private String disability;
  private String specialConsiderations;

  /**
   * Retrieves the formatted date of birth based on the day, month, and year values.
   *
   * @return The formatted date of birth (yyyy-MM-dd), or null if the date components are not valid
   *         integers.
   */
  public String getDateOfBirth() {
    try {
      int year = Integer.parseInt(dobYear);
      int month = Integer.parseInt(dobMonth);
      int day = Integer.parseInt(dobDay);

      return String.format("%d-%02d-%02d", year, month, day);
    } catch (NumberFormatException e) {
      throw new CaabApplicationException("Unable to format date of birth", e);
    }
  }


}
