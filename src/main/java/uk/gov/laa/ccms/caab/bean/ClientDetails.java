package uk.gov.laa.ccms.caab.bean;

import lombok.Data;

/**
 * Represents the details of a client.
 */
@Data
public class ClientDetails {

  //basic details
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
  private Boolean vulnerableClient = false;
  private Boolean highProfileClient = false;
  private Boolean vexatiousLitigant = false;
  private Boolean mentalIncapacity = false;

  //contact details
  private String telephoneHome;
  private String telephoneWork;
  private String telephoneMobile;
  private String emailAddress;
  private String password;
  private String passwordReminder;
  private String correspondenceMethod;
  private String correspondenceLanguage;

  //address details
  private Boolean noFixedAbode = false;
  private String country;
  private String houseNameNumber;
  private String postcode;
  private String addressLine1;
  private String addressLine2;
  private String cityTown;
  private String county;

  //address search details
  private String uprn;
  private boolean noAddressLookup = false;

  //equal opportunities monitoring
  private String ethnicOrigin;
  private String disability;
  private String specialConsiderations;


}
