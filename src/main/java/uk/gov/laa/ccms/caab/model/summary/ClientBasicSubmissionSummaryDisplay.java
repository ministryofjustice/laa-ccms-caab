package uk.gov.laa.ccms.caab.model.summary;

import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@Data
public class ClientBasicSubmissionSummaryDisplay {

  private CommonLookupValueDetail title;

  private String surname;

  private String middleName;

  private String firstname;

  private String surnameAtBirth;

  private String dateOfBirth;

  private CommonLookupValueDetail countryOfOrigin;

  private String niNumber;

  private String homeOfficeNumber;

  private CommonLookupValueDetail gender;

  private CommonLookupValueDetail maritalStatus;

  private boolean highProfileClient;

  private boolean vulnerableClient;

  private boolean mentalIncapacity;

}
