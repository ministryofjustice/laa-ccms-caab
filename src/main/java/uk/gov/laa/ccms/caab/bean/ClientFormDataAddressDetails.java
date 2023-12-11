package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the client address details form.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClientFormDataAddressDetails extends AbstractClientFormData {

  private Boolean noFixedAbode = false;
  private String country;
  private String houseNameNumber;
  private String postcode;
  private String addressLine1;
  private String addressLine2;
  private String cityTown;
  private String county;

  //Required for address searching
  private ClientFormDataAddressSearch addressSearch;

}
