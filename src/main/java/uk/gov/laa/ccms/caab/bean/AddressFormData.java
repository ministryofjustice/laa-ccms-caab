package uk.gov.laa.ccms.caab.bean;

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
  private String houseNameNumber;

  /**
   * The postal code for the address.
   */
  private String postcode;

  /**
   * The care of (c/o) field, indicating the person or organization to whom the mail should be
   * delivered at the address.
   */
  private String careOf;

  /**
   * The first line of the address.
   * This usually contains street information.
   */
  private String addressLine1;

  /**
   * The second line of the address.
   * This can be used for additional street or area information.
   */
  private String addressLine2;

  /**
   * The city or town part of the address.
   */
  private String cityTown;

  /**
   * The county or regional division of the address.
   */
  private String county;
}
