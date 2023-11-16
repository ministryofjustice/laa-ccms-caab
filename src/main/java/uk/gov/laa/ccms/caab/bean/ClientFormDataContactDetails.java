package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

@Data

public class ClientFormDataContactDetails {

  private String telephoneHome;
  private String telephoneWork;
  private String telephoneMobile;
  private String emailAddress;
  private String password;
  private String passwordReminder;
  private String correspondenceMethod;
  private String correspondenceLanguage;

  //Required for validation
  private Boolean vulnerableClient = false;

  //Required for template rendering
  private String clientFlowFormAction;

}
