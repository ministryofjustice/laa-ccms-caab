package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientPersonalDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContactDetail;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;

public class ClientDetailMapperTest {

  private ClientDetailMapper clientDetailMapper;

  // Private variables for test values
  private final String title = "MR";
  private final String surname = "TEST";
  private String countryOfOrigin = "UK";
  private final String gender = "MALE";
  private final String maritalStatus = "SINGLE";
  private final String telephoneHome = "1111111111";
  private final String telephoneWork = "2222222222";
  private final String telephoneMobile = "3333333333";
  private final String emailAddress = "test@test.com";
  private String password = "password";
  private String passwordReminder = "reminder";
  private final String correspondenceMethod = "LETTER";
  private final String correspondenceLanguage = "GBR";
  private boolean vulnerableClient = false;
  private boolean noFixedAbode = false;
  private final String country = "GBR";
  private final String houseNameNumber = "1234";
  private final String postcode = "SW1A 1AA";
  private final String addressLine1 = "Address Line 1";
  private final String cityTown = "CITY";
  private final String ethnicOrigin = "TEST";
  private final String disability = "TEST";
  private final String specialConsiderations = "TEST SPECIAL CONSIDERATIONS";

  private String day = "10";
  private String month = "06";
  private String year = "2000";


  @BeforeEach
  void setUp() {
    clientDetailMapper = Mappers.getMapper(ClientDetailMapper.class);
  }

  @Test
  void testToSoaClientDetail() {
    // Create a ClientDetails object for testing
    ClientDetails clientDetails = buildClientDetails();

    ClientDetail clientDetail = clientDetailMapper.toSoaClientDetail(clientDetails);

    assertEquals(title, clientDetail.getDetails().getName().getTitle());
    assertEquals(surname, clientDetail.getDetails().getName().getSurname());

    // Assertions for personal information
    assertNotNull(clientDetail.getDetails().getPersonalInformation().getDateOfBirth());  // assuming a non-null date is set
    assertEquals(gender, clientDetail.getDetails().getPersonalInformation().getGender());
    assertEquals(maritalStatus, clientDetail.getDetails().getPersonalInformation().getMaritalStatus());

    // Assertions for contact details
    assertEquals(telephoneHome, clientDetail.getDetails().getContacts().getTelephoneHome());
    assertEquals(telephoneWork, clientDetail.getDetails().getContacts().getTelephoneWork());
    assertEquals(telephoneMobile, clientDetail.getDetails().getContacts().getMobileNumber());
    assertEquals(emailAddress, clientDetail.getDetails().getContacts().getEmailAddress());

    // Assertions for correspondence details
    assertEquals(correspondenceMethod, clientDetail.getDetails().getContacts().getCorrespondenceMethod());
    assertEquals(correspondenceLanguage, clientDetail.getDetails().getContacts().getCorrespondenceLanguage());

    // Assertions for disability and ethnic monitoring
    assertEquals(Collections.singletonList(disability), clientDetail.getDetails().getDisabilityMonitoring().getDisabilityType());
    assertEquals(ethnicOrigin, clientDetail.getDetails().getEthnicMonitoring());

    // Assertions for special considerations
    assertEquals(specialConsiderations, clientDetail.getDetails().getSpecialConsiderations());

    // Assertions for address details
    assertEquals(houseNameNumber, clientDetail.getDetails().getAddress().getHouse());
    assertEquals(addressLine1, clientDetail.getDetails().getAddress().getAddressLine1());
    assertEquals(cityTown, clientDetail.getDetails().getAddress().getCity());
    assertEquals(country, clientDetail.getDetails().getAddress().getCountry());
    assertEquals(postcode, clientDetail.getDetails().getAddress().getPostalCode());

  }

  @Test
  void testMapStringToListNonNullValue() {
    String inputValue = "testString";
    List<String> expectedOutput = Collections.singletonList(inputValue);
    List<String> result = clientDetailMapper.mapStringToList(inputValue);
    assertEquals(expectedOutput, result);
  }

  @Test
  void testMapStringToListNullValue() {
    List<String> result = clientDetailMapper.mapStringToList( null);
    assertNull(result);
  }

  @ParameterizedTest
  @CsvSource({
      "1, 1, 2000, 1, 0, 2000",
      "15, 7, 1990, 15, 6, 1990",
      "31, 12, 2022, 31, 11, 2022"
  })
  void testMapDateOfBirth(String day, String month, String year, int expectedDay, int expectedMonth, int expectedYear) {
    // Create a ClientDetails object for testing
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setDobDay(day);
    clientDetails.setDobMonth(month);
    clientDetails.setDobYear(year);

    // Perform the mapping
    Date dateOfBirth = clientDetailMapper.mapDateOfBirth(clientDetails);

    // Get Calendar instance for assertions
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateOfBirth);

    // Assertions for the mapped date
    assertEquals(expectedYear, calendar.get(Calendar.YEAR));
    assertEquals(expectedMonth, calendar.get(Calendar.MONTH));
    assertEquals(expectedDay, calendar.get(Calendar.DAY_OF_MONTH));
  }

  @Test
  void testMapDateOfBirthWithNull() {
    assertNull(clientDetailMapper.mapDateOfBirth(null));
  }

  @ParameterizedTest
  @CsvSource({
      "John, James, TEST, John James TEST",
      "John, , TEST, John TEST",
      ", James, TEST, James TEST",
      "John, James,, John James",
      ",,,",
  })
  void testMapFullName(String firstName, String middleNames, String surname, String expectedFullName) {
    // Create a ClientDetails object for testing
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setFirstName(firstName);
    clientDetails.setMiddleNames(middleNames);
    clientDetails.setSurname(surname);

    // Perform the mapping
    String fullName = clientDetailMapper.mapFullName(clientDetails);

    // Assertions for the mapped full name
    assertEquals(expectedFullName, fullName);
  }

  @Test
  void testToClientDetails_nameDetails(){
    NameDetail nameDetail = new NameDetail();
    nameDetail.setTitle("MR");
    nameDetail.setFirstName("FIRSTNAME");
    nameDetail.setSurname("SURNAME");
    nameDetail.setMiddleName("MIDDLENAME");
    nameDetail.setSurnameAtBirth("SURNAMEATBIRTH");

    ClientDetailDetails details = new ClientDetailDetails();
    details.setName(nameDetail);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertEquals("MR", clientDetails.getTitle());
    assertEquals("FIRSTNAME", clientDetails.getFirstName());
    assertEquals("MIDDLENAME", clientDetails.getMiddleNames());
    assertEquals("SURNAME", clientDetails.getSurname());
    assertEquals("SURNAMEATBIRTH", clientDetails.getSurnameAtBirth());
  }

  @Test
  void testToClientDetails_nameDetailFieldsAreNull(){
    NameDetail nameDetail = new NameDetail();
    nameDetail.setTitle(null);
    nameDetail.setFirstName(null);
    nameDetail.setSurname(null);
    nameDetail.setMiddleName(null);
    nameDetail.setSurnameAtBirth(null);

    ClientDetailDetails details = new ClientDetailDetails();
    details.setName(nameDetail);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertNull(clientDetails.getTitle());
    assertNull(clientDetails.getFirstName());
    assertNull(clientDetails.getMiddleNames());
    assertNull(clientDetails.getSurname());
    assertNull(clientDetails.getSurnameAtBirth());
  }

  @Test
  void testToClientDetails_nameDetailsAreNull(){
    ClientDetailDetails details = new ClientDetailDetails();
    details.setName(null);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertNull(clientDetails.getTitle());
    assertNull(clientDetails.getFirstName());
    assertNull(clientDetails.getMiddleNames());
    assertNull(clientDetails.getSurname());
    assertNull(clientDetails.getSurnameAtBirth());
  }

  @Test
  void testToClientDetails_personalDetails(){
    ClientPersonalDetail personalInformation = new ClientPersonalDetail();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
    Date dateOfBirth = calendar.getTime();

    personalInformation.setDateOfBirth(dateOfBirth);
    personalInformation.setCountryOfOrigin("Country");
    personalInformation.setNationalInsuranceNumber("NI123456C");
    personalInformation.setHomeOfficeNumber("HO123456");
    personalInformation.setMaritalStatus("Single");
    personalInformation.setVulnerableClient(true);
    personalInformation.setHighProfileClient(false);
    personalInformation.setVexatiousLitigant(false);
    personalInformation.setMentalCapacityInd(true);

    ClientDetailDetails details = new ClientDetailDetails();
    details.setPersonalInformation(personalInformation);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertEquals("1",clientDetails.getDobDay());
    assertEquals("1", clientDetails.getDobMonth());
    assertEquals("2000", clientDetails.getDobYear());
    assertEquals("Country", clientDetails.getCountryOfOrigin());
    assertEquals("NI123456C", clientDetails.getNationalInsuranceNumber());
    assertEquals("HO123456", clientDetails.getHomeOfficeNumber());
    assertEquals("Single", clientDetails.getMaritalStatus());
    assertTrue(clientDetails.getVulnerableClient());
    assertFalse(clientDetails.getHighProfileClient());
    assertFalse(clientDetails.getVexatiousLitigant());
    assertTrue(clientDetails.getMentalIncapacity());
  }

  @Test
  void testToClientDetails_personalDetailsFieldsAreNull(){
    ClientPersonalDetail personalInformation = new ClientPersonalDetail();
    personalInformation.setDateOfBirth(null);
    personalInformation.setCountryOfOrigin(null);
    personalInformation.setNationalInsuranceNumber(null);
    personalInformation.setHomeOfficeNumber(null);
    personalInformation.setMaritalStatus(null);
    personalInformation.setVulnerableClient(null);
    personalInformation.setHighProfileClient(null);
    personalInformation.setVexatiousLitigant(null);
    personalInformation.setMentalCapacityInd(null);

    ClientDetailDetails details = new ClientDetailDetails();
    details.setPersonalInformation(personalInformation);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertNull(clientDetails.getDobDay());
    assertNull(clientDetails.getDobMonth());
    assertNull(clientDetails.getDobYear());
    assertNull(clientDetails.getCountryOfOrigin());
    assertNull(clientDetails.getNationalInsuranceNumber());
    assertNull(clientDetails.getHomeOfficeNumber());
    assertNull(clientDetails.getGender());
    assertNull(clientDetails.getMaritalStatus());
    assertNull(clientDetails.getVulnerableClient());
    assertNull(clientDetails.getHighProfileClient());
    assertNull(clientDetails.getVexatiousLitigant());
    assertNull(clientDetails.getMentalIncapacity());
  }

  @Test
  void testToClientDetails_personalDetailsAreNull(){
    ClientDetailDetails details = new ClientDetailDetails();
    details.setPersonalInformation(null);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertNull(clientDetails.getDobDay());
    assertNull(clientDetails.getDobMonth());
    assertNull(clientDetails.getDobYear());
    assertNull(clientDetails.getCountryOfOrigin());
    assertNull(clientDetails.getNationalInsuranceNumber());
    assertNull(clientDetails.getHomeOfficeNumber());
    assertNull(clientDetails.getGender());
    assertNull(clientDetails.getMaritalStatus());
    assertNull(clientDetails.getVulnerableClient());
    assertNull(clientDetails.getHighProfileClient());
    assertNull(clientDetails.getVexatiousLitigant());
    assertNull(clientDetails.getMentalIncapacity());
  }

  @Test
  void testToClientDetails_contactDetails(){
    ContactDetail contactDetail = new ContactDetail();
    contactDetail.setTelephoneHome("1234567890");
    contactDetail.setTelephoneWork("234567890");
    contactDetail.setMobileNumber("34567890");
    contactDetail.setEmailAddress("email@address.com");
    contactDetail.setPasswordReminder("Password");
    contactDetail.setCorrespondenceLanguage("language");
    contactDetail.setCorrespondenceMethod("method");

    ClientDetailDetails details = new ClientDetailDetails();
    details.setContacts(contactDetail);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertEquals("1234567890",clientDetails.getTelephoneHome());
    assertEquals("234567890", clientDetails.getTelephoneWork());
    assertEquals("34567890", clientDetails.getTelephoneMobile());
    assertEquals("email@address.com", clientDetails.getEmailAddress());
    assertEquals("Password", clientDetails.getPasswordReminder());
    assertEquals("language", clientDetails.getCorrespondenceLanguage());
    assertEquals("method", clientDetails.getCorrespondenceMethod());
  }

  @Test
  void testToClientDetails_contactDetailsFieldsAreNull(){
    ContactDetail contactDetail = new ContactDetail();
    contactDetail.setTelephoneHome(null);
    contactDetail.setTelephoneWork(null);
    contactDetail.setMobileNumber(null);
    contactDetail.setEmailAddress(null);
    contactDetail.setPasswordReminder(null);
    contactDetail.setCorrespondenceLanguage(null);
    contactDetail.setCorrespondenceMethod(null);

    ClientDetailDetails details = new ClientDetailDetails();
    details.setContacts(contactDetail);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertNull(clientDetails.getTelephoneHome());
    assertNull(clientDetails.getTelephoneWork());
    assertNull(clientDetails.getTelephoneMobile());
    assertNull(clientDetails.getEmailAddress());
    assertNull(clientDetails.getPasswordReminder());
    assertNull(clientDetails.getCorrespondenceMethod());
    assertNull(clientDetails.getCorrespondenceLanguage());
  }

  @Test
  void testToClientDetails_contactDetailsAreNull(){
    ClientDetailDetails details = new ClientDetailDetails();
    details.setContacts(null);

    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(details);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertNull(clientDetails.getTelephoneHome());
    assertNull(clientDetails.getTelephoneWork());
    assertNull(clientDetails.getTelephoneMobile());
    assertNull(clientDetails.getEmailAddress());
    assertNull(clientDetails.getPasswordReminder());
    assertNull(clientDetails.getCorrespondenceMethod());
    assertNull(clientDetails.getCorrespondenceLanguage());
  }



  @Test
  void testToClientDetails_detailsAreNull(){
    ClientDetail clientDetail = new ClientDetail();
    clientDetail.setDetails(null);

    ClientDetails clientDetails = clientDetailMapper.toClientDetails(clientDetail);

    assertNull(clientDetails.getTitle());
    assertNull(clientDetails.getFirstName());
    assertNull(clientDetails.getMiddleNames());
    assertNull(clientDetails.getSurname());
    assertNull(clientDetails.getSurnameAtBirth());
    assertNull(clientDetails.getDobDay());
    assertNull(clientDetails.getDobMonth());
    assertNull(clientDetails.getDobYear());
    assertNull(clientDetails.getCountryOfOrigin());
    assertNull(clientDetails.getNationalInsuranceNumber());
    assertNull(clientDetails.getHomeOfficeNumber());
    assertNull(clientDetails.getGender());
    assertNull(clientDetails.getMaritalStatus());
    assertNull(clientDetails.getVulnerableClient());
    assertNull(clientDetails.getHighProfileClient());
    assertNull(clientDetails.getVexatiousLitigant());
    assertNull(clientDetails.getMentalIncapacity());
    assertNull(clientDetails.getTelephoneHome());
    assertNull(clientDetails.getTelephoneWork());
    assertNull(clientDetails.getTelephoneMobile());
    assertNull(clientDetails.getEmailAddress());
    assertNull(clientDetails.getPasswordReminder());
    assertNull(clientDetails.getCorrespondenceMethod());
    assertNull(clientDetails.getCorrespondenceLanguage());
    assertNull(clientDetails.getNoFixedAbode());
    assertNull(clientDetails.getCountry());
    assertNull(clientDetails.getEthnicOrigin());
    assertNull(clientDetails.getDisability());
    assertNull(clientDetails.getSpecialConsiderations());
    assertNull(clientDetails.getHouseNameNumber());
    assertNull(clientDetails.getAddressLine1());
    assertNull(clientDetails.getAddressLine2());
    assertNull(clientDetails.getCityTown());
    assertNull(clientDetails.getPostcode());
    assertNull(clientDetails.getCounty());
  }

  @Test
  void testToClientDetails_null(){
    ClientDetails clientDetails = clientDetailMapper.toClientDetails(null);
    assertNull(clientDetails);
  }

  private ClientDetails buildClientDetails() {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setTitle(title);
    clientDetails.setSurname(surname);
    clientDetails.setCountryOfOrigin(countryOfOrigin);
    clientDetails.setGender(gender);
    clientDetails.setMaritalStatus(maritalStatus);

    clientDetails.setDobDay(day);
    clientDetails.setDobMonth(month);
    clientDetails.setDobYear(year);

    clientDetails.setTelephoneHome(telephoneHome);
    clientDetails.setTelephoneWork(telephoneWork);
    clientDetails.setTelephoneMobile(telephoneMobile);
    clientDetails.setEmailAddress(emailAddress);
    clientDetails.setPassword(password);
    clientDetails.setPasswordReminder(passwordReminder);
    clientDetails.setCorrespondenceMethod(correspondenceMethod);
    clientDetails.setCorrespondenceLanguage(correspondenceLanguage);

    clientDetails.setVulnerableClient(vulnerableClient);
    clientDetails.setNoFixedAbode(noFixedAbode);
    clientDetails.setCountry(country);
    clientDetails.setHouseNameNumber(houseNameNumber);
    clientDetails.setPostcode(postcode);
    clientDetails.setAddressLine1(addressLine1);
    clientDetails.setCityTown(cityTown);

    clientDetails.setEthnicOrigin(ethnicOrigin);
    clientDetails.setDisability(disability);
    clientDetails.setSpecialConsiderations(specialConsiderations);
    return clientDetails;
  }


}