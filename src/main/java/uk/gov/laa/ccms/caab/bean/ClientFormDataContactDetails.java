package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the client contact details form.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClientFormDataContactDetails extends AbstractClientFormData {

  @Size(max = 15)
  private String telephoneHome;

  @Size(max = 15)
  private String telephoneWork;

  @Size(max = 15)
  private String telephoneMobile;

  @Size(max = 200)
  private String emailAddress;

  @Size(max = 35)
  private String password;

  @Size(max = 35)
  private String passwordReminder;

  private String correspondenceMethod;
  private String correspondenceLanguage;

  private boolean telephoneHomePresent = false;
  private boolean telephoneWorkPresent = false;
  private boolean telephoneMobilePresent = false;

  /**
   * Cleanup mobile numbers if not selected in the UI.
   */
  public void clearUnsetPhoneNumbers() {
    telephoneHome = telephoneHomePresent ? telephoneHome : null;
    telephoneWork = telephoneWorkPresent ? telephoneWork : null;
    telephoneMobile = telephoneMobilePresent ? telephoneMobile : null;
  }

}
