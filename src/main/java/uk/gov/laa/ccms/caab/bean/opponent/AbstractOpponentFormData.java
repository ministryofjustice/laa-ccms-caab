package uk.gov.laa.ccms.caab.bean.opponent;

import lombok.Data;

/**
 * Represents the basic opponent details stored during opponent creation/edit flows.
 */
@Data
public abstract class AbstractOpponentFormData {
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
   * The work telephone number for the opponent.
   */
  private String telephoneWork;

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
}
