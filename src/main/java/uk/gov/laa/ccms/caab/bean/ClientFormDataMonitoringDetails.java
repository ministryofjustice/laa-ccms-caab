package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the client monitoring details form.
 */
@Data
public class ClientFormDataMonitoringDetails {

  private String ethnicOrigin;
  private String disability;
  private String specialConsiderations;

  //Required for template rendering
  private String clientFlowFormAction;
}
