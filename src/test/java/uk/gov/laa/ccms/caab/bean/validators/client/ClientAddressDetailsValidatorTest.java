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
class ClientAddressDetailsValidatorTest {

  @InjectMocks
  private ClientAddressDetailsValidator clientAddressDetailsValidator;

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
    assertTrue(clientAddressDetailsValidator.supports(ClientDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientAddressDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertFalse(errors.hasErrors());
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
    clientDetails.setVulnerableClient(true);

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

    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_countryRequired(String country) {
    clientDetails.setCountry(country);
    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.country", errors.getFieldError("country").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_houseNameNumberRequired(String houseNameNumber) {
    clientDetails.setHouseNameNumber(houseNameNumber);
    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("houseNameNumber"));
    assertEquals("required.houseNameNumber", errors.getFieldError("houseNameNumber").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_addressLine1Required(String addressLine1) {
    clientDetails.setAddressLine1(addressLine1);
    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("addressLine1"));
    assertEquals("required.addressLine1", errors.getFieldError("addressLine1").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_cityTownRequired(String cityTown) {
    clientDetails.setCityTown(cityTown);
    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("cityTown"));
    assertEquals("required.cityTown", errors.getFieldError("cityTown").getCode());
  }

  // Test for UK postcodes
  @ParameterizedTest
  @NullAndEmptySource
  public void validate_postcodeRequired_UK(String postcode) {
    clientDetails.setCountry("GBR");
    clientDetails.setPostcode(postcode);
    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("required.postcode", errors.getFieldError("postcode").getCode());
  }

  @ParameterizedTest
  @CsvSource({
      "USA",
      "GBR"
  })
  public void validate_validatePostcodeFormat(String country) {
    clientDetails.setCountry(country);
    clientDetails.setPostcode("@@@@@@");
    clientAddressDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("invalid.format", errors.getFieldError("postcode").getCode());
  }

  private ClientDetails buildClientDetails() {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setVulnerableClient(false);
    clientDetails.setNoFixedAbode(false);
    clientDetails.setCountry("GBR");
    clientDetails.setHouseNameNumber("1234");
    clientDetails.setPostcode("SW1A 1AA");
    clientDetails.setAddressLine1("Address Line 1");
    clientDetails.setCityTown("CITY");
    return clientDetails;
  }
}