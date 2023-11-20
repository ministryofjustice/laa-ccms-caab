package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the client monitoring details form.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClientFormDataMonitoringDetails extends ClientAbstractFormData{

  private String ethnicOrigin;
  private String disability;
  private String specialConsiderations;
}
