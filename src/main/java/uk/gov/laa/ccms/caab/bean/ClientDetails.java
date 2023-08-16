package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the details of a client.
 */
@Data
public class ClientDetails {
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

  private boolean vulnerableClient;

  private boolean highProfileClient;

  private boolean vexatiousLitigant;

  private boolean mentalIncapacity;
}
