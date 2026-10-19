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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;

@ExtendWith(MockitoExtension.class)
class FindAddressValidatorTest {

  @InjectMocks private FindAddressValidator findAddressValidator;

  @Mock private ClientFormDataAddressDetails addressDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    addressDetails = buildAddressDetails();
    errors = new BeanPropertyBindingResult(addressDetails, "addressDetails");
  }

  @Test
  public void supports_ReturnsTrueForClientDetailsClass() {
    assertTrue(findAddressValidator.supports(ClientFormDataAddressDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(findAddressValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    findAddressValidator.validate(addressDetails, errors);
    System.out.println(errors.getAllErrors());
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_postcodeRequired(final String postcode) {
    addressDetails.setPostcode(postcode);
    findAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("required.postcode", errors.getFieldError("postcode").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @CsvSource({"sa58ab", "SA58AB", "SA5 8AB"})
  public void validate_acceptsFullPostcodeWithOrWithoutSpace(final String postcode) {
    addressDetails.setPostcode(postcode);
    findAddressValidator.validate(addressDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_rejectsInvalidPostcode() {
    addressDetails.setPostcode("SA5");
    findAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("invalid.postcode", errors.getFieldError("postcode").getCode());
    assertEquals("Enter a full UK postcode", errors.getFieldError("postcode").getDefaultMessage());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  public void validate_rejectsNonUkCountryForLookup() {
    addressDetails.setCountry("USA");
    findAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals("required.GBR", errors.getFieldError("country").getCode());
  }

  @Test
  public void validate_buildingNameNumberIsOptional() {
    addressDetails.setHouseNameNumber(null);
    findAddressValidator.validate(addressDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @CsvSource({
    "country, , 1",
    "country, test, 3",
    "country, GBR, 2",
    "houseNameNumber, test, 2",
    "postcode, SW1A 1AA, 1",
    "addressLine1, test, 2",
    "addressLine2, test, 2",
    "cityTown, test, 2",
    "county, test, 2"
  })
  public void validate_noFixedAbode_invalid(String field, String value, int numberOfErrors) {
    addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setNoFixedAbode(true);

    if ("country".equals(field)) {
      addressDetails.setCountry(value);
    } else if ("houseNameNumber".equals(field)) {
      addressDetails.setHouseNameNumber(value);
    } else if ("postcode".equals(field)) {
      addressDetails.setPostcode(value);
    } else if ("addressLine1".equals(field)) {
      addressDetails.setAddressLine1(value);
    } else if ("addressLine2".equals(field)) {
      addressDetails.setAddressLine2(value);
    } else if ("cityTown".equals(field)) {
      addressDetails.setCityTown(value);
    } else if ("county".equals(field)) {
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
