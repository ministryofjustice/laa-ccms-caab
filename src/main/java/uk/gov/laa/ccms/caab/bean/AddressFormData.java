package uk.gov.laa.ccms.caab.bean;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.ADDRESS_LINE1_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.DEFAULT_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.POSTCODE_CHARACTER_SIZE;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Represents an address details form.
 */
@Data
public class AddressFormData {

  /**
   * The preferred address.
   */
  private String preferredAddress;

  /**
   * The country where the address is located.
   */
  private String country;

  /**
   * The house name or number.
   * This field typically contains the building number or name in the address.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String houseNameNumber;

  /**
   * The postal code for the address.
   */
  @Size(max = POSTCODE_CHARACTER_SIZE)
  private String postcode;

  /**
   * The care of (c/o) field, indicating the person or organization to whom the mail should be
   * delivered at the address.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String careOf;

  /**
   * The first line of the address.
   * This usually contains street information.
   */
  @Size(max = ADDRESS_LINE1_CHARACTER_SIZE)
  private String addressLine1;

  /**
   * The second line of the address.
   * This can be used for additional street or area information.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String addressLine2;

  /**
   * The city or town part of the address.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String cityTown;

  /**
   * The county or regional division of the address.
   */
  @Size(max = DEFAULT_CHARACTER_SIZE)
  private String county;
}
