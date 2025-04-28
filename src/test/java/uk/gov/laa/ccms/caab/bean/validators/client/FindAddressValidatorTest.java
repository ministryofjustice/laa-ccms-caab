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
class FindAddressValidatorTest {

  @InjectMocks
  private FindAddressValidator findAddressValidator;

  @Mock
  private ClientFormDataAddressDetails addressDetails;

  private Errors errors;

  @BeforeEach
  void setUp() {
    addressDetails = buildAddressDetails();
    errors = new BeanPropertyBindingResult(addressDetails, "addressDetails");
  }

  @Test
  void supports_ReturnsTrueForClientDetailsClass() {
    assertTrue(findAddressValidator.supports(ClientFormDataAddressDetails.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(findAddressValidator.supports(Object.class));
  }

  @Test
  void validate() {
    findAddressValidator.validate(addressDetails, errors);
    System.out.println(errors.getAllErrors());
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_countryRequired(final String country) {
    addressDetails.setCountry(country);
    findAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.country", errors.getFieldError("country").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validate_countryNotUnitedKingdom() {
    addressDetails.setCountry("USA");
    findAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.GBR", errors.getFieldError("country").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_houseNameNumberRequired(String houseNameNumber) {
    addressDetails.setHouseNameNumber(houseNameNumber);
    findAddressValidator.validate(addressDetails, errors);
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
  void validate_noFixedAbode_invalid(String field, String value, int numberOfErrors) {
    addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setNoFixedAbode(true);

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

    findAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(numberOfErrors, errors.getErrorCount());
  }

  private ClientFormDataAddressDetails buildAddressDetails() {
    ClientFormDataAddressDetails formDataAddressDetails = new ClientFormDataAddressDetails();
    formDataAddressDetails.setNoFixedAbode(false);
    formDataAddressDetails.setCountry("GBR");
    formDataAddressDetails.setHouseNameNumber("1234");
    formDataAddressDetails.setPostcode("SW1A 1AA");
    formDataAddressDetails.setAddressLine1("Address line 1");
    formDataAddressDetails.setCityTown("CITY");
    return formDataAddressDetails;
  }
}