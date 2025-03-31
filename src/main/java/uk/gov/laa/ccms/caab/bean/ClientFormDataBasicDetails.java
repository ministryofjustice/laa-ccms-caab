package uk.gov.laa.ccms.caab.bean;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.laa.ccms.caab.bean.common.Individual;

/**
 * Represents the client basic details form.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ClientFormDataBasicDetails extends AbstractClientFormData implements Individual {

  private String title;

  @Size(max = 35)
  private String surname;

  @Size(max = 35)
  private String firstName;

  @Size(max = 35)
  private String middleNames;
  private String surnameAtBirth;

  private String dateOfBirth;
  private String countryOfOrigin;

  @Size(max = 9)
  private String nationalInsuranceNumber;

  @Size(max = 35)
  private String homeOfficeNumber;
  private String gender;
  private String maritalStatus;
  private Boolean highProfileClient = false;
  private Boolean vexatiousLitigant = false;
  private Boolean mentalIncapacity = false;
}
