package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

public class ClientDetailMapperTest {

  private ClientDetailMapper clientDetailMapper;

  // Private variables for test values
  private String title = "MR";
  private String surname = "TEST";
  private String countryOfOrigin = "UK";
  private String gender = "MALE";
  private String maritalStatus = "SINGLE";
  private String telephoneHome = "1111111111";
  private String telephoneWork = "2222222222";
  private String telephoneMobile = "3333333333";
  private String emailAddress = "test@test.com";
  private String password = "password";
  private String passwordReminder = "reminder";
  private String correspondenceMethod = "LETTER";
  private String correspondenceLanguage = "GBR";
  private boolean vulnerableClient = false;
  private boolean noFixedAbode = false;
  private String country = "GBR";
  private String houseNameNumber = "1234";
  private String postcode = "SW1A 1AA";
  private String addressLine1 = "Address Line 1";
  private String cityTown = "CITY";
  private String ethnicOrigin = "TEST";
  private String disability = "TEST";
  private String specialConsiderations = "TEST SPECIAL CONSIDERATIONS";

  @BeforeEach
  void setUp() {
    clientDetailMapper = Mappers.getMapper(ClientDetailMapper.class);
  }

//  @Test
//  void testToSoaClientDetail() {
//    // Create a ClientDetails object for testing
//    ClientDetails clientDetails = buildClientDetails();
//
//  }

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

  private ClientDetails buildClientDetails() {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setTitle(title);
    clientDetails.setSurname(surname);
    clientDetails.setCountryOfOrigin(countryOfOrigin);
    clientDetails.setGender(gender);
    clientDetails.setMaritalStatus(maritalStatus);

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