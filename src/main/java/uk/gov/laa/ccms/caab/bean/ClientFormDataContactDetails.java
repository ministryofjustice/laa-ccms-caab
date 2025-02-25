package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the client contact details form.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClientFormDataContactDetails extends AbstractClientFormData {

  private String telephoneHome;
  private String telephoneWork;
  private String telephoneMobile;
  private String emailAddress;
  private String password;
  private String passwordReminder;
  private String correspondenceMethod;
  private String correspondenceLanguage;

  private boolean telephoneHomePresent = false;
  private boolean telephoneWorkPresent = false;
  private boolean telephoneMobilePresent = false;

}
