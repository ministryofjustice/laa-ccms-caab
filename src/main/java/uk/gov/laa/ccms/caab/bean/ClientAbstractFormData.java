package uk.gov.laa.ccms.caab.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ClientAbstractFormData {

  //Required for validation
  protected Boolean vulnerableClient = false;

  //Required for template rendering
  protected String clientFlowFormAction;
}
