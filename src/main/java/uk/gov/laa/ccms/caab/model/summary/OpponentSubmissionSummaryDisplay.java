package uk.gov.laa.ccms.caab.model.summary;

import java.util.Date;
import lombok.Data;

/**
 * Represents the opponent submission summary display.
 */
@Data
public class OpponentSubmissionSummaryDisplay {

  /**
   * The type of the opponent.
   */
  private String type;

  /**
   * The title of the opponent.
   */
  private String title;

  /**
   * The surname of the opponent.
   */
  private String surname;

  /**
   * The first name of the opponent.
   */
  private String firstName;

  /**
   * The middle names of the opponent.
   */
  private String middleNames;

  /**
   * The date of birth of the opponent.
   */
  private Date dateOfBirth;

  /**
   * The relationship of the opponent to the case.
   */
  private String relationshipToCase;

  /**
   * The relationship of the opponent to the client.
   */
  private String relationshipToClient;

  /**
   * The national insurance number of the opponent.
   */
  private String nationalInsuranceNumber;

  /**
   * The house name or number of the opponent's address.
   */
  private String houseNameOrNumber;

  /**
   * The first line of the opponent's address.
   */
  private String addressLine1;

  /**
   * The second line of the opponent's address.
   */
  private String addressLine2;

  /**
   * The city of the opponent's address.
   */
  private String city;

  /**
   * The county of the opponent's address.
   */
  private String county;

  /**
   * The country of the opponent's address.
   */
  private String country;

  /**
   * The postcode of the opponent's address.
   */
  private String postcode;

  /**
   * The home telephone number of the opponent.
   */
  private String telephoneHome;

  /**
   * The work telephone number of the opponent.
   */
  private String telephoneWork;

  /**
   * The mobile telephone number of the opponent.
   */
  private String telephoneMobile;

  /**
   * The fax number of the opponent.
   */
  private String faxNumber;

  /**
   * The email address of the opponent.
   */
  private String emailAddress;

  /**
   * Whether the opponent is legally aided.
   */
  private Boolean legalAided;

  /**
   * The certificate number of the opponent.
   */
  private String certificateNumber;

  /**
   * The name of the opponent's organisation.
   */
  private String organisationName;

  /**
   * The type of the opponent's organisation.
   */
  private String organisationType;

  /**
   * Whether the opponent's organisation is currently trading.
   */
  private Boolean currentlyTrading;

  /**
   * The role of the contact person in the opponent's organisation.
   */
  private String contactNameRole;

  /**
   * Other information about the opponent.
   */
  private String otherInformation;
}
