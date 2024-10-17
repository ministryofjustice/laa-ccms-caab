package uk.gov.laa.ccms.caab.bean.request;

import lombok.Data;

/**
 * Holds the flow data for a provider request, including the request type form data.
 */
@Data
public class ProviderRequestFlowFormData {


  private ProviderRequestTypeFormData requestTypeFormData;

  public ProviderRequestFlowFormData() {
    this.requestTypeFormData = new ProviderRequestTypeFormData();
  }


}
