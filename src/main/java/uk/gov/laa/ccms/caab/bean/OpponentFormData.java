package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the opponent details stored during opponent creation/edit flows.
 */
@Data
public class OpponentFormData {


  /**
   * The partyId of the opponent.
   */
  private String partyId;

  /**
   * The type of Opponent.
   */
  private String type;

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
  private boolean currentlyTrading;

  /**
   * The opponent's relationship to the case.
   */
  private String relationshipToCase;

  /**
   * The opponent's relationship to the client.
   */
  private String relationshipToClient;

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
   * Flag to indicate this is a shared opponent.
   */
  private boolean shared;


}
