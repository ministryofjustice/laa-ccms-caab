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
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;

@ExtendWith(SpringExtension.class)
class ClientAddressDetailsValidatorTest {

  @InjectMocks
  private ClientAddressDetailsValidator clientAddressDetailsValidator;

  @Mock
  private ClientFormDataAddressDetails addressDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    addressDetails = buildClientFormDataAddressDetails();
    errors = new BeanPropertyBindingResult(addressDetails, "addressDetails");
  }

  @Test
  public void supports_ReturnsTrueForClientFormDataAddressDetailsClass() {
    assertTrue(clientAddressDetailsValidator.supports(ClientFormDataAddressDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientAddressDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @CsvSource({
      "country, test, 1",
      "country, GBR, 1",
      "houseNameNumber, test, 1",
      "postcode, SW1A 1AA, 1",
      "addressLine1, test, 1",
      "addressLine2, test, 1",
      "cityTown, test, 1",
      "county, test, 1"
  })
  public void validate_noFixedAbode_invalid(String field, String value, int numberOfErrors) {
    addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setNoFixedAbode(true);
    addressDetails.setVulnerableClient(true);

    if (field.equals("country")) {
      addressDetails.setCountry(value);
    } else if (field.equals("houseNameNumber")) {
      addressDetails.setHouseNameNumber(value);
    } else if (field.equals("postcode")) {
      addressDetails.setPostcode(value);
    } else if (field.equals("addressLine1")) {
      addressDetails.setAddressLine1(value);
    } else if (field.equals("addressLine2")) {
      addressDetails.setAddressLine2(value);
    } else if (field.equals("cityTown")) {
      addressDetails.setCityTown(value);
    } else if (field.equals("county")) {
      addressDetails.setCounty(value);
    }

    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(numberOfErrors, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_countryRequired(String country) {
    addressDetails.setCountry(country);
    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.country", errors.getFieldError("country").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_houseNameNumberRequired(String houseNameNumber) {
    addressDetails.setHouseNameNumber(houseNameNumber);
    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("houseNameNumber"));
    assertEquals("required.houseNameNumber", errors.getFieldError("houseNameNumber").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_addressLine1Required(String addressLine1) {
    addressDetails.setAddressLine1(addressLine1);
    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("addressLine1"));
    assertEquals("required.addressLine1", errors.getFieldError("addressLine1").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_cityTownRequired(String cityTown) {
    addressDetails.setCityTown(cityTown);
    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("cityTown"));
    assertEquals("required.cityTown", errors.getFieldError("cityTown").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  // Test for UK postcodes
  @ParameterizedTest
  @NullAndEmptySource
  public void validate_postcodeRequired_UK(String postcode) {
    addressDetails.setCountry("GBR");
    addressDetails.setPostcode(postcode);
    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("required.postcode", errors.getFieldError("postcode").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @CsvSource({
      "USA",
      "GBR"
  })
  public void validate_validatePostcodeFormat(String country) {
    addressDetails.setCountry(country);
    addressDetails.setPostcode("@@@@@@");
    clientAddressDetailsValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("invalid.format", errors.getFieldError("postcode").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  private ClientFormDataAddressDetails buildClientFormDataAddressDetails() {
    ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setVulnerableClient(false);
    addressDetails.setNoFixedAbode(false);
    addressDetails.setCountry("GBR");
    addressDetails.setHouseNameNumber("1234");
    addressDetails.setPostcode("SW1A 1AA");
    addressDetails.setAddressLine1("Address Line 1");
    addressDetails.setCityTown("CITY");
    return addressDetails;
  }
}