package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the client details stored during client creation/edit flows.
 */
@Data
public class ClientFlowFormData {

  private String action;

  private ClientFormDataBasicDetails basicDetails;

  private ClientFormDataDeceasedDetails deceasedDetails;

  private ClientFormDataContactDetails contactDetails;

  private ClientFormDataAddressDetails addressDetails;

  private ClientFormDataMonitoringDetails monitoringDetails;

  public ClientFlowFormData(String action) {
    this.action = action;
  }

}
