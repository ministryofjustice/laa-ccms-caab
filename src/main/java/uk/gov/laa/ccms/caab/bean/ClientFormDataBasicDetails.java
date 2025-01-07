package uk.gov.laa.ccms.caab.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the client basic details form.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClientFormDataBasicDetails extends AbstractClientFormData {

  private String title;
  private String surname;
  private String firstName;
  private String middleNames;
  private String surnameAtBirth;

  private String dateOfBirth;
  private String countryOfOrigin;
  private String nationalInsuranceNumber;
  private String homeOfficeNumber;
  private String gender;
  private String maritalStatus;
  private Boolean highProfileClient = false;
  private Boolean vexatiousLitigant = false;
  private Boolean mentalIncapacity = false;
}
