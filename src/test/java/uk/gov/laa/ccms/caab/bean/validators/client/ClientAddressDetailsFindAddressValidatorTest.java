package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;

@ExtendWith(SpringExtension.class)
class ClientAddressDetailsFindAddressValidatorTest {

  @InjectMocks
  private ClientAddressDetailsFindAddressValidator clientAddressDetailsFindAddressValidator;

  @Mock
  private ClientDetails clientDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    clientDetails = buildClientDetails();
    errors = new BeanPropertyBindingResult(clientDetails, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForClientDetailsClass() {
    assertTrue(clientAddressDetailsFindAddressValidator.supports(ClientDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientAddressDetailsFindAddressValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    clientAddressDetailsFindAddressValidator.validate(clientDetails, errors);
    System.out.println(errors.getAllErrors());
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_countryRequired(String country) {
    clientDetails.setCountry(country);
    clientAddressDetailsFindAddressValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.country", errors.getFieldError("country").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  public void validate_countryNotUnitedKingdom() {;
    clientDetails.setCountry("USA");
    clientAddressDetailsFindAddressValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.GBR", errors.getFieldError("country").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_houseNameNumberRequired(String houseNameNumber) {
    clientDetails.setHouseNameNumber(houseNameNumber);
    clientAddressDetailsFindAddressValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("houseNameNumber"));
    assertEquals("required.houseNameNumber", errors.getFieldError("houseNameNumber").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @CsvSource({
      "country, , 2",
      "country, test, 3",
      "country, GBR, 3",
      "houseNameNumber, test, 2",
      "postcode, SW1A 1AA, 3",
      "addressLine1, test, 3",
      "addressLine2, test, 3",
      "cityTown, test, 3",
      "county, test, 3"
  })
  public void validate_noFixedAbode_invalid(String field, String value, int numberOfErrors) {
    clientDetails = new ClientDetails();
    clientDetails.setNoFixedAbode(true);

    if (field.equals("country")) {
      clientDetails.setCountry(value);
    } else if (field.equals("houseNameNumber")) {
      clientDetails.setHouseNameNumber(value);
    } else if (field.equals("postcode")) {
      clientDetails.setPostcode(value);
    } else if (field.equals("addressLine1")) {
      clientDetails.setAddressLine1(value);
    } else if (field.equals("addressLine2")) {
      clientDetails.setAddressLine2(value);
    } else if (field.equals("cityTown")) {
      clientDetails.setCityTown(value);
    } else if (field.equals("county")) {
      clientDetails.setCounty(value);
    }

    clientAddressDetailsFindAddressValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(numberOfErrors, errors.getErrorCount());
  }

  private ClientDetails buildClientDetails() {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setNoFixedAbode(false);
    clientDetails.setCountry("GBR");
    clientDetails.setHouseNameNumber("1234");
    clientDetails.setPostcode("SW1A 1AA");
    clientDetails.setAddressLine1("Address line 1");
    clientDetails.setCityTown("CITY");
    return clientDetails;
  }
}