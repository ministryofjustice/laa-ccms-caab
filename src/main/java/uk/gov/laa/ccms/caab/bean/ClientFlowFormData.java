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

  public ClientFlowFormData(final String action) {
    this.action = action;
  }

  /**
   * Overrides the Default setter, so we can set the vulnerable clients for other child objects.
   */
  public void setBasicDetails(final ClientFormDataBasicDetails basicDetails) {
    this.basicDetails = basicDetails;
    if (this.contactDetails != null) {
      this.contactDetails.setVulnerableClient(basicDetails.getVulnerableClient());

      if (this.addressDetails != null) {
        this.addressDetails.setVulnerableClient(basicDetails.getVulnerableClient());
      }
    }
  }

}
