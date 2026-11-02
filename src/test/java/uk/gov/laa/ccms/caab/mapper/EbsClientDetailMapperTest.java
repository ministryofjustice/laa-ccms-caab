package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.data.model.AddressDetail;
import uk.gov.laa.ccms.data.model.ClientDetail;
import uk.gov.laa.ccms.data.model.ContactDetails;
import uk.gov.laa.ccms.data.model.DisabilityMonitoringDetail;
import uk.gov.laa.ccms.data.model.NameDetail;

class EbsClientDetailMapperTest {

  private EbsClientDetailMapper mapper;

  // Private variables for test values
  private final String title = "mr";

  private final String firstname = "firstname";

  private final String middleNames = "middle names";
  private final String surname = "surname";
  private final String surnameAtBirth = "surnameAtBirth";

  private final String countryOfOrigin = "UK";
  private final String countryOfOriginDescription = "United Kingdom";
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

  private final String dateofBirth = "10/06/2000";
  private final String day = "10";
  private final String month = "06";
  private final String year = "2000";

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(EbsClientDetailMapper.class);
  }

  @Test
  void toClientFlowFormData() {
    ClientFlowFormData expectedClientFlowFormData = buildClientFlowFormData();
    // null out action as it's not mapped backwards
    expectedClientFlowFormData.setAction(null);

    ClientDetail clientDetail = buildClientDetail();

    ClientFlowFormData clientFlowFormData = mapper.toClientFlowFormData(clientDetail);

    assertThat(clientFlowFormData).isEqualTo(expectedClientFlowFormData);
  }

  @Test
  void toClientFlowFormData_null() {
    ClientFlowFormData clientFlowFormData = mapper.toClientFlowFormData(null);
    assertNull(clientFlowFormData);
  }

  @Test
  void toClientFlowFormData_monitoringDetails_null() {
    ClientDetail clientDetail = buildClientDetail();
    clientDetail.setDisabilityMonitoring(null);

    ClientFlowFormData clientFlowFormData = mapper.toClientFlowFormData(clientDetail);

    assertNull(clientFlowFormData.getMonitoringDetails().getDisability());
  }

  @Test
  void toClientFlowFormData_address_null() {
    ClientDetail clientDetail = buildClientDetail();
    clientDetail.setAddress(null);

    ClientFlowFormData clientFlowFormData = mapper.toClientFlowFormData(clientDetail);

    assertThat(clientFlowFormData.getAddressDetails().getVulnerableClient()).isTrue();
    assertNull(clientFlowFormData.getAddressDetails().getPostcode());
  }

  @Test
  void toClientFormDataBasicDetails() {
    ClientFormDataBasicDetails clientFormDataBasicDetails =
        mapper.toClientFormDataBasicDetails(buildClientDetail());

    assertThat(clientFormDataBasicDetails).isEqualTo(buildClientFormDataBasicDetails());
  }

  @Test
  void toClientFormDataBasicDetails_nameAndDateOfBirth_null() {
    ClientDetail clientDetail = buildClientDetail();
    clientDetail.setName(null);
    clientDetail.setDateOfBirth(null);

    ClientFormDataBasicDetails basicDetails = mapper.toClientFormDataBasicDetails(clientDetail);

    assertNull(basicDetails.getTitle());
    assertNull(basicDetails.getFirstName());
    assertNull(basicDetails.getMiddleNames());
    assertNull(basicDetails.getSurname());
    assertNull(basicDetails.getSurnameAtBirth());
    assertNull(basicDetails.getDateOfBirth());
  }

  @Test
  void testToClientFormDataContactDetails() {
    ClientFormDataContactDetails expectedClientFormDataContactDetails =
        buildClientFormDataContactDetails();

    ClientFormDataContactDetails clientFormDataContactDetails =
        mapper.toClientFormDataContactDetails(buildContactDetails());

    assertThat(clientFormDataContactDetails).isEqualTo(expectedClientFormDataContactDetails);
  }

  @Test
  void testToClientFormDataContactDetails_null() {
    ClientFormDataContactDetails contactDetails = mapper.toClientFormDataContactDetails(null);
    assertNull(contactDetails);
  }

  @Test
  void testToClientFormDataAddressDetails() {
    ClientFormDataAddressDetails expectedClientFormDataAddressDetails =
        buildClientFormDataAddressDetails();

    ClientFormDataAddressDetails clientFormDataAddressDetails =
        mapper.toClientFormDataAddressDetails(buildAddressDetail());

    assertThat(clientFormDataAddressDetails).isEqualTo(expectedClientFormDataAddressDetails);
  }

  @Test
  void testToClientFormDataAddressDetails_null() {
    ClientFormDataAddressDetails addressDetails = mapper.toClientFormDataAddressDetails(null);
    assertNull(addressDetails);
  }

  @Test
  void testToClientFlowFormData_null() {
    ClientFlowFormData clientFlowFormData = mapper.toClientFlowFormData(null);
    assertNull(clientFlowFormData);
  }

  @Test
  void mapComponentDateFromLocalDate() {
    assertThat(mapper.mapComponentDateFromLocalDate(LocalDate.of(2000, 6, 10)))
        .isEqualTo(dateofBirth);
  }

  @Test
  void mapComponentDateFromLocalDate_null() {
    assertNull(mapper.mapComponentDateFromLocalDate(null));
  }

  // EBS Helper methods

  private ClientDetail buildClientDetail() {
    return new ClientDetail()
        .name(
            new NameDetail()
                .title(title)
                .firstName(firstname)
                .middleName(middleNames)
                .surname(surname)
                .surnameAtBirth(surnameAtBirth)
                .fullName(firstname + " " + middleNames + " " + surname))
        .dateOfBirth(
            LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day)))
        .countryOfOriginCode(countryOfOrigin)
        .countryOfOriginDescription(countryOfOriginDescription)
        .gender(gender)
        .maritalStatus(maritalStatus)
        .niNumber(nationalInsuranceNumber)
        .homeOfficeNumber(homeOfficeNumber)
        .vulnerableClient(clientStatuses)
        .highProfileClient(clientStatuses)
        .vexatiousLitigant(clientStatuses)
        .mentalCapacityInd(clientStatuses)
        .contacts(buildContactDetails())
        .address(buildAddressDetail())
        .noFixedAbode(noFixedAbode)
        .disabilityMonitoring(new DisabilityMonitoringDetail().addDisabilityTypeItem(disability))
        .ethnicMonitoring(ethnicOrigin)
        .specialConsiderations(specialConsiderations);
  }

  private ContactDetails buildContactDetails() {
    return new ContactDetails()
        .telephoneHome(telephoneHome)
        .telephoneWork(telephoneWork)
        .mobileNumber(telephoneMobile)
        .emailAddress(emailAddress)
        .password(password)
        .passwordReminder(passwordReminder)
        .correspondenceLanguage(correspondenceLanguage)
        .correspondenceMethod(correspondenceMethod);
  }

  private AddressDetail buildAddressDetail() {
    return new AddressDetail()
        .country(country)
        .house(houseNameNumber)
        .postalCode(postcode)
        .addressLine1(addressLine1)
        .addressLine2(addressLine2)
        .city(cityTown);
  }

  // CAAB Helper methods

  private ClientFlowFormData buildClientFlowFormData() {
    ClientFlowFormData clientFlowFormData = new ClientFlowFormData("create");
    clientFlowFormData.setBasicDetails(buildClientFormDataBasicDetails());
    clientFlowFormData.setContactDetails(buildClientFormDataContactDetails());
    clientFlowFormData.setAddressDetails(buildClientFormDataAddressDetails());
    clientFlowFormData.setMonitoringDetails(buildClientFormDataMonitoringDetails());
    return clientFlowFormData;
  }

  private ClientFormDataBasicDetails buildClientFormDataBasicDetails() {
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    addPersonalInformationToClientFormDataBasicDetails(basicDetails);
    addNameDetailToClientFormDataBasicDetails(basicDetails);
    return basicDetails;
  }

  private void addPersonalInformationToClientFormDataBasicDetails(
      ClientFormDataBasicDetails basicDetails) {
    basicDetails.setDateOfBirth(dateofBirth);
    basicDetails.setCountryOfOrigin(countryOfOrigin);
    basicDetails.setCountryOfOriginDescription(countryOfOriginDescription);
    basicDetails.setGender(gender);
    basicDetails.setMaritalStatus(maritalStatus);
    basicDetails.setNationalInsuranceNumber(nationalInsuranceNumber);
    basicDetails.setHomeOfficeNumber(homeOfficeNumber);
    basicDetails.setVulnerableClient(clientStatuses);
    basicDetails.setHighProfileClient(clientStatuses);
    basicDetails.setVexatiousLitigant(clientStatuses);
    basicDetails.setMentalIncapacity(clientStatuses);
  }

  private void addNameDetailToClientFormDataBasicDetails(ClientFormDataBasicDetails basicDetails) {
    basicDetails.setTitle(title);
    basicDetails.setFirstName(firstname);
    basicDetails.setMiddleNames(middleNames);
    basicDetails.setSurnameAtBirth(surnameAtBirth);
    basicDetails.setSurname(surname);
  }

  private ClientFormDataContactDetails buildClientFormDataContactDetails() {
    ClientFormDataContactDetails contactDetails = new ClientFormDataContactDetails();
    contactDetails.setTelephoneHome(telephoneHome);
    contactDetails.setTelephoneWork(telephoneWork);
    contactDetails.setTelephoneMobile(telephoneMobile);
    contactDetails.setTelephoneHomePresent(true);
    contactDetails.setTelephoneWorkPresent(true);
    contactDetails.setTelephoneMobilePresent(true);
    contactDetails.setEmailAddress(emailAddress);
    contactDetails.setPassword(password);
    contactDetails.setPasswordReminder(passwordReminder);
    contactDetails.setCorrespondenceMethod(correspondenceMethod);
    contactDetails.setCorrespondenceLanguage(correspondenceLanguage);
    return contactDetails;
  }

  private ClientFormDataAddressDetails buildClientFormDataAddressDetails() {
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

  private ClientFormDataMonitoringDetails buildClientFormDataMonitoringDetails() {
    ClientFormDataMonitoringDetails monitoringDetails = new ClientFormDataMonitoringDetails();
    monitoringDetails.setEthnicOrigin(ethnicOrigin);
    monitoringDetails.setDisability(disability);
    monitoringDetails.setSpecialConsiderations(specialConsiderations);
    return monitoringDetails;
  }
}
