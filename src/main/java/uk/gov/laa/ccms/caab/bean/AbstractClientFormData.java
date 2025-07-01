package uk.gov.laa.ccms.caab.bean;

import lombok.Getter;
import lombok.Setter;

/** Represents the abstract superclass client details form. */
@Getter
@Setter
public abstract class AbstractClientFormData {

  // Required for validation
  protected Boolean vulnerableClient = false;

  // Required for template rendering
  protected String clientFlowFormAction;
}
