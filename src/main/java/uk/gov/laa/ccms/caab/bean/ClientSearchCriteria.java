package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;
import uk.gov.laa.ccms.caab.bean.common.Individual;

/** Represents the details used for client search. */
@Data
public class ClientSearchCriteria implements Serializable, Individual {

  /** The forename of the client. */
  @Size(max = 35)
  String forename;

  /** The surname of the client. */
  @Size(max = 35)
  String surname;

  /** The day of birth of the client. */
  String dateOfBirth;

  /** The gender of the client. */
  String gender;

  /** The type of unique identifier for the client. */
  Integer uniqueIdentifierType;

  /** The value of the unique identifier for the client. */
  @Size(max = 35)
  String uniqueIdentifierValue;

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
