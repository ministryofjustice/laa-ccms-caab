package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetailsDisabilityMonitoring;
import uk.gov.laa.ccms.soa.gateway.model.ClientPersonalDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContactDetail;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;

public class ClientDetailMapperTest {

  private ClientDetailMapper clientDetailMapper;

  // Private variables for test values
  private final String title = "mr";

  private final String firstname = "firstname";

  private final String middleNames = "middle names";
  private final String surname = "surname";
  private final String surnameAtBirth = "surnameAtBirth";

  private final String countryOfOrigin = "UK";
  private final String gender = "MALE";
  private final String maritalStatus = "SINGLE";

  private final String nationalInsuranceNumber = "NI123456NI";
  private final String homeOfficeNumber = "HO123456HO";
  private final String telephoneHome = "1111111111";
  private final String telephoneWork = "2222222222";
  private final String telephoneMobile = "3333333333";
  private final String emailAddress = "test@test.com";
  private final String password = "password";
  private final String passwordReminder = "reminder";
  private final String correspondenceMethod = "LETTER";
  private final String correspondenceLanguage = "GBR";
  private final boolean clientStatuses = true;
  private final boolean noFixedAbode = false;
  private final String country = "GBR";
  private final String houseNameNumber = "1234";
  private final String postcode = "SW1A 1AA";
  private final String addressLine1 = "Address Line 1";
  private final String addressLine2 = "Address Line 2";
  private final String cityTown = "CITY";
  private final String ethnicOrigin = "TEST";
  private final String disability = "TEST";
  private final String specialConsiderations = "TEST SPECIAL CONSIDERATIONS";

  private String day = "10";
  private String month = "6";
  private String year = "2000";


  @BeforeEach
  void setUp() {
    clientDetailMapper = Mappers.getMapper(ClientDetailMapper.class);
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
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setDobDay(day);
    basicDetails.setDobMonth(month);
    basicDetails.setDobYear(year);

    // Perform the mapping
    Date dateOfBirth = clientDetailMapper.mapDateOfBirth(basicDetails);

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
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setFirstName(firstName);
    basicDetails.setMiddleNames(middleNames);
    basicDetails.setSurname(surname);

    // Perform the mapping
    String fullName = clientDetailMapper.mapFullName(basicDetails);

    // Assertions for the mapped full name
    assertEquals(expectedFullName, fullName);
  }

  @Test
  void toClientDetail() {
    ClientDetail expectedClientDetail = new ClientDetail();
    expectedClientDetail.setDetails(buildClientDetailDetails());

    ClientFlowFormData clientFlowFormData = buildClientFlowFormData();

    ClientDetail clientDetail = clientDetailMapper.toClientDetail(clientFlowFormData);
    assertEquals(expectedClientDetail, clientDetail);
  }

  @Test
  void toClientDetail_null() {
    ClientDetail clientDetail = clientDetailMapper.toClientDetail(null);
    assertNull(clientDetail);
  }

  @Test
  void toClientDetailDetails() {
    ClientDetailDetails expectedClientDetailDetails = buildClientDetailDetails();

    ClientFlowFormData clientFlowFormData = buildClientFlowFormData();

    ClientDetailDetails clientDetailDetails = clientDetailMapper.toClientDetailDetails(clientFlowFormData);
    assertEquals(expectedClientDetailDetails, clientDetailDetails);
  }

  @Test
  void toClientDetailDetails_ethnicMonitoring_null() {
    ClientFlowFormData clientFlowFormData = buildClientFlowFormData();
    clientFlowFormData.getMonitoringDetails().setEthnicOrigin(null);

    ClientDetailDetails clientDetailDetails = clientDetailMapper.toClientDetailDetails(clientFlowFormData);

    assertNull(clientDetailDetails.getEthnicMonitoring());
  }

  @Test
  void toClientDetailDetails_specialConsiderations_null() {
    ClientFlowFormData clientFlowFormData = buildClientFlowFormData();
    clientFlowFormData.getMonitoringDetails().setSpecialConsiderations(null);

    ClientDetailDetails clientDetailDetails = clientDetailMapper.toClientDetailDetails(clientFlowFormData);

    assertNull(clientDetailDetails.getSpecialConsiderations());
  }

  @Test
  void toClientDetailDetails_disabilityMonitoring_null() {
    ClientFlowFormData clientFlowFormData = buildClientFlowFormData();
    clientFlowFormData.getMonitoringDetails().setDisability(null);

    ClientDetailDetails clientDetailDetails = clientDetailMapper.toClientDetailDetails(clientFlowFormData);

    assertNull(clientDetailDetails.getDisabilityMonitoring().getDisabilityType());
  }

  @Test
  void toClientDetailDetails_null() {
    ClientDetailDetails clientDetailDetails = clientDetailMapper.toClientDetailDetails(null);
    assertNull(clientDetailDetails);
  }


  @Test
  void toNameDetail() {
    NameDetail expectedNameDetail = buildNameDetail();

    ClientFormDataBasicDetails clientFormDataBasicDetails = new ClientFormDataBasicDetails();
    addNameDetailToClientFormDataBasicDetails(clientFormDataBasicDetails);

    NameDetail nameDetail = clientDetailMapper.toNameDetail(clientFormDataBasicDetails);

    assertEquals(expectedNameDetail, nameDetail);
  }

  @Test
  void toNameDetail_null() {
    NameDetail nameDetail = clientDetailMapper.toNameDetail(null);
    assertNull(nameDetail);
  }

  @Test
  void toClientPersonalDetail() {
    ClientPersonalDetail expectedPersonalDetail = buildClientPersonalDetail();

    ClientFormDataBasicDetails clientFormDataBasicDetails = new ClientFormDataBasicDetails();
    addPersonalInformationToClientFormDataBasicDetails(clientFormDataBasicDetails);

    ClientPersonalDetail personalDetail = clientDetailMapper.toClientPersonalDetail(clientFormDataBasicDetails);

    assertEquals(expectedPersonalDetail, personalDetail);
  }

  @Test
  void toClientPersonalDetail_null() {
    ClientPersonalDetail personalDetail = clientDetailMapper.toClientPersonalDetail(null);
    assertNull(personalDetail);
  }

  @Test
  void toContactDetails() {
    ContactDetail expectedContactDetail = buildContactDetail();
    ClientFormDataContactDetails clientFormDataContactDetails = buildClientFormDataContactDetails();

    ContactDetail contactDetail = clientDetailMapper.toContactDetails(clientFormDataContactDetails);

    assertEquals(expectedContactDetail, contactDetail);
  }

  @Test
  void toContactDetails_null() {
    ContactDetail contactDetail = clientDetailMapper.toContactDetails(null);
    assertNull(contactDetail);
  }

  @Test
  void toAddressDetail() {
    AddressDetail expectedAddressDetail = buildAddressDetail();
    ClientFormDataAddressDetails clientFormDataAddressDetails = buildClientFormDataAddressDetails();

    AddressDetail addressDetail = clientDetailMapper.toAddressDetail(clientFormDataAddressDetails);

    assertEquals(expectedAddressDetail, addressDetail);
  }

  @Test
  void toAddressDetail_null() {
    AddressDetail addressDetail = clientDetailMapper.toAddressDetail(null);
    assertNull(addressDetail);
  }

  @Test
  void toClientFlowFormData() {
    ClientFlowFormData expectedClientFlowFormData = buildClientFlowFormData();
    //null out action as it's not mapped backwards
    expectedClientFlowFormData.setAction(null);

    ClientDetailDetails clientDetailDetails = buildClientDetailDetails();

    ClientFlowFormData clientFlowFormData = clientDetailMapper.toClientFlowFormData(clientDetailDetails);

    assertEquals(expectedClientFlowFormData, clientFlowFormData);
  }

  @Test
  void toClientFlowFormData_null() {
    ClientFlowFormData clientFlowFormData = clientDetailMapper.toClientFlowFormData(null);
    assertNull(clientFlowFormData);
  }

  @Test
  void toClientFlowFormData_monitoringDetails_null(){
    ClientDetailDetails clientDetailDetails = buildClientDetailDetails();
    clientDetailDetails.setDisabilityMonitoring(null);

    ClientFlowFormData clientFlowFormData = clientDetailMapper.toClientFlowFormData(clientDetailDetails);

    assertNull(clientFlowFormData.getMonitoringDetails().getDisability());
  }

  @Test
  void addClientFormDataBasicDetailsFromNameDetail(){
    ClientFormDataBasicDetails expectedClientFormDataBasicDetails = new ClientFormDataBasicDetails();
    addNameDetailToClientFormDataBasicDetails(expectedClientFormDataBasicDetails);

    NameDetail nameDetail = buildNameDetail();
    ClientFormDataBasicDetails clientFormDataBasicDetails = new ClientFormDataBasicDetails();

    clientDetailMapper.addClientFormDataBasicDetailsFromNameDetail(clientFormDataBasicDetails, nameDetail);

    assertEquals(expectedClientFormDataBasicDetails, clientFormDataBasicDetails);
  }


  @Test
  void addClientFormDataBasicDetailsFromNameDetail_null(){
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();

    clientDetailMapper.addClientFormDataBasicDetailsFromNameDetail(basicDetails, null);

    assertNull(basicDetails.getTitle());
    assertNull(basicDetails.getFirstName());
    assertNull(basicDetails.getMiddleNames());
    assertNull(basicDetails.getSurname());
    assertNull(basicDetails.getSurnameAtBirth());
  }

  @Test
  void addClientFormDataBasicDetailsFromClientPersonalDetail(){
    ClientFormDataBasicDetails expectedClientFormDataBasicDetails = new ClientFormDataBasicDetails();
    addPersonalInformationToClientFormDataBasicDetails(expectedClientFormDataBasicDetails);

    ClientPersonalDetail clientPersonalDetail = buildClientPersonalDetail();
    ClientFormDataBasicDetails clientFormDataBasicDetails = new ClientFormDataBasicDetails();

    clientDetailMapper.addClientFormDataBasicDetailsFromClientPersonalDetail(clientFormDataBasicDetails, clientPersonalDetail);

    assertEquals(expectedClientFormDataBasicDetails, clientFormDataBasicDetails);
  }


  @Test
  void addClientFormDataBasicDetailsFromClientPersonalDetail_null(){
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();

    clientDetailMapper.addClientFormDataBasicDetailsFromClientPersonalDetail(basicDetails, null);

    assertNull(basicDetails.getDobDay());
    assertNull(basicDetails.getDobMonth());
    assertNull(basicDetails.getDobYear());
    assertNull(basicDetails.getCountryOfOrigin());
    assertNull(basicDetails.getNationalInsuranceNumber());
    assertNull(basicDetails.getHomeOfficeNumber());
    assertNull(basicDetails.getGender());
    assertNull(basicDetails.getMaritalStatus());
    assertFalse(basicDetails.getVulnerableClient());
    assertFalse(basicDetails.getHighProfileClient());
    assertFalse(basicDetails.getVexatiousLitigant());
    assertFalse(basicDetails.getMentalIncapacity());
  }

  @Test
  void testToClientFormDataContactDetails(){
    ClientFormDataContactDetails expectedClientFormDataContactDetails = buildClientFormDataContactDetails();
    ContactDetail contactDetail = buildContactDetail();

    ClientFormDataContactDetails clientFormDataContactDetails = clientDetailMapper.toClientFormDataContactDetails(contactDetail);

    assertEquals(expectedClientFormDataContactDetails, clientFormDataContactDetails);
  }

  @Test
  void testToClientFormDataContactDetails_null(){
    ClientFormDataContactDetails contactDetails = clientDetailMapper.toClientFormDataContactDetails(null);
    assertNull(contactDetails);
  }

  @Test
  void testToClientFormDataAddressDetails(){
    ClientFormDataAddressDetails expectedClientFormDataAddressDetails = buildClientFormDataAddressDetails();
    AddressDetail addressDetail = buildAddressDetail();

    ClientFormDataAddressDetails clientFormDataAddressDetails = clientDetailMapper.toClientFormDataAddressDetails(addressDetail);

    assertEquals(expectedClientFormDataAddressDetails, clientFormDataAddressDetails);
  }

  @Test
  void testToClientFormDataAddressDetails_null(){
    ClientFormDataAddressDetails addressDetails = clientDetailMapper.toClientFormDataAddressDetails(null);
    assertNull(addressDetails);
  }

  @Test
  void testToClientFlowFormData_null(){
    ClientFlowFormData clientFlowFormData = clientDetailMapper.toClientFlowFormData(null);
    assertNull(clientFlowFormData);
  }


  //SOA Helper methods

  private ClientDetailDetails buildClientDetailDetails(){
    ClientDetailDetails clientDetailDetails = new ClientDetailDetails();
    clientDetailDetails.setNoFixedAbode(false);
    clientDetailDetails.setName(buildNameDetail());
    clientDetailDetails.setPersonalInformation(buildClientPersonalDetail());
    clientDetailDetails.setContacts(buildContactDetail());
    clientDetailDetails.setAddress(buildAddressDetail());
    clientDetailDetails.setDisabilityMonitoring(buildDisabilityMonitoring());
    clientDetailDetails.setEthnicMonitoring(ethnicOrigin);
    clientDetailDetails.setSpecialConsiderations(specialConsiderations);
    return clientDetailDetails;
  }

  private NameDetail buildNameDetail() {
    NameDetail nameDetail = new NameDetail();
    nameDetail.setTitle(title);
    nameDetail.setFirstName(firstname);
    nameDetail.setSurname(surname);
    nameDetail.setMiddleName(middleNames);
    nameDetail.setSurnameAtBirth(surnameAtBirth);
    nameDetail.setFullName(firstname + " " + middleNames + " " + surname);
    return nameDetail;
  }

  private ClientPersonalDetail buildClientPersonalDetail() {
    ClientPersonalDetail personalInformation = new ClientPersonalDetail();

    LocalDate localDate = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
    Date dateOfBirth = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

    personalInformation.setDateOfBirth(dateOfBirth);
    personalInformation.setCountryOfOrigin(countryOfOrigin);
    personalInformation.setGender(gender);
    personalInformation.setNationalInsuranceNumber(nationalInsuranceNumber);
    personalInformation.setHomeOfficeNumber(homeOfficeNumber);
    personalInformation.setMaritalStatus(maritalStatus);
    personalInformation.setVulnerableClient(clientStatuses);
    personalInformation.setHighProfileClient(clientStatuses);
    personalInformation.setVexatiousLitigant(clientStatuses);
    personalInformation.setMentalCapacityInd(clientStatuses);
    return personalInformation;
  }

  private ContactDetail buildContactDetail() {
    ContactDetail contactDetail = new ContactDetail();
    contactDetail.setTelephoneHome(telephoneHome);
    contactDetail.setTelephoneWork(telephoneWork);
    contactDetail.setMobileNumber(telephoneMobile);
    contactDetail.setEmailAddress(emailAddress);
    contactDetail.setPassword(password);
    contactDetail.setPasswordReminder(passwordReminder);
    contactDetail.setCorrespondenceLanguage(correspondenceLanguage);
    contactDetail.setCorrespondenceMethod(correspondenceMethod);
    return contactDetail;
  }

  private AddressDetail buildAddressDetail(){
    AddressDetail addressDetail = new AddressDetail();
    addressDetail.setCountry(country);
    addressDetail.setHouse(houseNameNumber);
    addressDetail.setPostalCode(postcode);
    addressDetail.setAddressLine1(addressLine1);
    addressDetail.setAddressLine2(addressLine2);
    addressDetail.setCity(cityTown);
    return addressDetail;
  }

  private ClientDetailDetailsDisabilityMonitoring buildDisabilityMonitoring(){
    ClientDetailDetailsDisabilityMonitoring disabilityMonitoring = new ClientDetailDetailsDisabilityMonitoring();
    List<String> disabilities = new ArrayList<>();
    disabilities.add(disability);
    disabilityMonitoring.setDisabilityType(disabilities);
    return disabilityMonitoring;
  }

  //CAAB Helper methods

  private ClientFlowFormData buildClientFlowFormData() {
    ClientFlowFormData clientFlowFormData = new ClientFlowFormData("create");
    clientFlowFormData.setBasicDetails(buildClientFormDataBasicDetails());
    clientFlowFormData.setContactDetails(buildClientFormDataContactDetails());
    clientFlowFormData.setAddressDetails(buildClientFormDataAddressDetails());
    clientFlowFormData.setMonitoringDetails(buildClientFormDataMonitoringDetails());
    return clientFlowFormData;
  }

  private ClientFormDataBasicDetails buildClientFormDataBasicDetails(){
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    addPersonalInformationToClientFormDataBasicDetails(basicDetails);
    addNameDetailToClientFormDataBasicDetails(basicDetails);
    return basicDetails;
  }

  private void addPersonalInformationToClientFormDataBasicDetails(ClientFormDataBasicDetails basicDetails){
    basicDetails.setDobDay(day);
    basicDetails.setDobMonth(month);
    basicDetails.setDobYear(year);
    basicDetails.setCountryOfOrigin(countryOfOrigin);
    basicDetails.setGender(gender);
    basicDetails.setMaritalStatus(maritalStatus);
    basicDetails.setNationalInsuranceNumber(nationalInsuranceNumber);
    basicDetails.setHomeOfficeNumber(homeOfficeNumber);
    basicDetails.setVulnerableClient(clientStatuses);
    basicDetails.setHighProfileClient(clientStatuses);
    basicDetails.setVexatiousLitigant(clientStatuses);
    basicDetails.setMentalIncapacity(clientStatuses);
  }

  private void addNameDetailToClientFormDataBasicDetails(ClientFormDataBasicDetails basicDetails){
    basicDetails.setTitle(title);
    basicDetails.setFirstName(firstname);
    basicDetails.setMiddleNames(middleNames);
    basicDetails.setSurnameAtBirth(surnameAtBirth);
    basicDetails.setSurname(surname);
  }

  private ClientFormDataContactDetails buildClientFormDataContactDetails(){
    ClientFormDataContactDetails contactDetails = new ClientFormDataContactDetails();
    contactDetails.setTelephoneHome(telephoneHome);
    contactDetails.setTelephoneWork(telephoneWork);
    contactDetails.setTelephoneMobile(telephoneMobile);
    contactDetails.setEmailAddress(emailAddress);
    contactDetails.setPassword(password);
    contactDetails.setPasswordReminder(passwordReminder);
    contactDetails.setCorrespondenceMethod(correspondenceMethod);
    contactDetails.setCorrespondenceLanguage(correspondenceLanguage);
    return contactDetails;
  }

  private ClientFormDataAddressDetails buildClientFormDataAddressDetails(){
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setVulnerableClient(false);
    addressDetails.setNoFixedAbode(noFixedAbode);
    addressDetails.setCountry(country);
    addressDetails.setHouseNameNumber(houseNameNumber);
    addressDetails.setPostcode(postcode);
    addressDetails.setAddressLine1(addressLine1);
    addressDetails.setAddressLine2(addressLine2);
    addressDetails.setCityTown(cityTown);
    return addressDetails;
  }

  private ClientFormDataMonitoringDetails buildClientFormDataMonitoringDetails(){
    ClientFormDataMonitoringDetails monitoringDetails = new ClientFormDataMonitoringDetails();
    monitoringDetails.setEthnicOrigin(ethnicOrigin);
    monitoringDetails.setDisability(disability);
    monitoringDetails.setSpecialConsiderations(specialConsiderations);
    return monitoringDetails;
  }


}