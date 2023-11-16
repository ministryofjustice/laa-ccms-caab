package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

@Data
public class ClientFormDataAddressDetails {

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

  //Required for validation
  private Boolean vulnerableClient = false;

  //Required for template rendering
  private String clientFlowFormAction;



}
