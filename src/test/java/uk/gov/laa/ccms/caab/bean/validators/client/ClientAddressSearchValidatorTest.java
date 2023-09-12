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
class ClientAddressSearchValidatorTest {

  @InjectMocks
  private ClientAddressSearchValidator clientAddressSearchValidator;

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
    assertTrue(clientAddressSearchValidator.supports(ClientDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientAddressSearchValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    clientAddressSearchValidator.validate(clientDetails, errors);
    System.out.println(errors.getAllErrors());
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_countryRequired(String country) {
    clientDetails.setCountry(country);
    clientAddressSearchValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.country", errors.getFieldError("country").getCode());
  }

  @Test
  public void validate_countryNotUnitedKingdom() {;
    clientDetails.setCountry("USA");
    clientAddressSearchValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.GBR", errors.getFieldError("country").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_houseNameNumberRequired(String houseNameNumber) {
    clientDetails.setHouseNameNumber(houseNameNumber);
    clientAddressSearchValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("houseNameNumber"));
    assertEquals("required.houseNameNumber", errors.getFieldError("houseNameNumber").getCode());
  }

  @ParameterizedTest
  @CsvSource({
      "country, test",
      "houseNameNumber, test",
      "postcode, SW1A 1AA",
      "addressLine1, test",
      "addressLine2, test",
      "cityTown, test",
      "county, test"
  })
  public void validate_noFixedAbode_invalid(String field, String value) {
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

    clientAddressSearchValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
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