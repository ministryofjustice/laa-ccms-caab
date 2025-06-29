package uk.gov.laa.ccms.caab.bean.opponent;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.ADDRESS_LINE1_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.DEFAULT_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.EMAIL_ADDRESS_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.OTHER_INFORMATION_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.POSTCODE_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.TELEPHONE_NUMBER_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Represents the basic opponent details stored during opponent creation/edit flows. */
@Data
public abstract class AbstractOpponentFormData {
  /** The id of the opponent. */
  private Integer id;

  /** The partyId of the opponent. */
  private String partyId;

  /** The party type of the Opponent. */
  private String type;

  /** The party name, which depends on the type of Opponent (Organisation or Individual). */
  private String partyName;

  /** The opponent's relationship to the case. */
  private String relationshipToCase;

  /** The display value for the opponent's relationship to the case. */
  private String relationshipToCaseDisplayValue;

  /** The opponent's relationship to the client. */
  private String relationshipToClient;

  /** The display value for the opponent's relationship to the client. */
  private String relationshipToClientDisplayValue;

  /** The opponent house name/number. */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String houseNameOrNumber;

  /** The address line 1 for the opponent. */
  @Size(max = ADDRESS_LINE1_CHARACTER_SIZE)
  private String addressLine1;

  /** The address line 2 for the opponent. */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String addressLine2;

  /** The city for the opponent. */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String city;

  /** The opponent's county. */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String county;

  /** The opponent's country. */
  private String country;

  /** The postcode for the opponent. */
  @Size(max = POSTCODE_CHARACTER_SIZE)
  private String postcode;

  /** The work telephone number for the opponent. */
  @Size(max = TELEPHONE_NUMBER_CHARACTER_SIZE)
  private String telephoneWork;

  /** The fax number for the opponent. */
  @Size(max = TELEPHONE_NUMBER_CHARACTER_SIZE)
  private String faxNumber;

  /** The email address for the opponent. */
  @Size(max = EMAIL_ADDRESS_CHARACTER_SIZE)
  private String emailAddress;

  /** Any other information regarding the opponent. */
  @Size(max = OTHER_INFORMATION_CHARACTER_SIZE)
  private String otherInformation;

  /** Flag to indicate that this opponent can be modified. */
  private Boolean editable;

  /** Flag to indicate that this opponent can be deleted. */
  private Boolean deletable;

  /** The app mode flag. */
  private Boolean appMode;

  /** The amendment flag. */
  private Boolean amendment;
}
