package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.Size;
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

  @Size(max = 35)
  private String houseNameNumber;

  @Size(max = 15)
  private String postcode;

  private String addressLine1;
  private String addressLine2;

  @Size(max = 35)
  private String cityTown;

  @Size(max = 35)
  private String county;

  //Required for address searching
  private AddressSearchFormData addressSearch;

}
