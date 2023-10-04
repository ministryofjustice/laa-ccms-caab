package uk.gov.laa.ccms.caab.bean;


import java.io.Serializable;
import lombok.Data;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Represents the details used for client search.
 */
@Data
public class ClientSearchCriteria implements Serializable {

  /**
   * The forename of the client.
   */
  String forename;

  /**
   * The surname of the client.
   */
  String surname;

  /**
   * The day of birth of the client.
   */
  String dobDay;

  /**
   * The month of birth of the client.
   */
  String dobMonth;

  /**
   * The year of birth of the client.
   */
  String dobYear;

  /**
   * The gender of the client.
   */
  String gender;

  /**
   * The type of unique identifier for the client.
   */
  Integer uniqueIdentifierType;

  /**
   * The value of the unique identifier for the client.
   */
  String uniqueIdentifierValue;

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
      // Handle the exception if any of the dobYear, dobMonth, or dobDay is not a valid integer
      throw new CaabApplicationException("Unable to format date of birth", e);
    }
  }

  /**
   * Retrieves the unique identifier value based on the matching type.
   *
   * @param matchingType The type of unique identifier to match against.
   * @return The unique identifier value if it matches the provided type, otherwise null.
   */

  public String getUniqueIdentifier(Integer matchingType) {
    if (this.uniqueIdentifierType != null && this.uniqueIdentifierType == matchingType) {
      return uniqueIdentifierValue;
    } else {
      return null;
    }
  }
}
