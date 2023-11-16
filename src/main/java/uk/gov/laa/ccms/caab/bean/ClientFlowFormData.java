package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

@Data
public class ClientFlowFormData {

  private String action;

  private ClientFormDataBasicDetails basicDetails;

  private ClientFormDataContactDetails contactDetails;

  private ClientFormDataAddressDetails addressDetails;

  private ClientFormDataMonitoringDetails monitoringDetails;

  public ClientFlowFormData(String action){
    this.action = action;
  }

}
