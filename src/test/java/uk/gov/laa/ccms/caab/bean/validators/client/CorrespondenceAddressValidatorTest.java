package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AddressFormData;

@ExtendWith(SpringExtension.class)
class CorrespondenceAddressValidatorTest {

  @InjectMocks
  private CorrespondenceAddressValidator correspondenceAddressValidator;

  @Mock
  private AddressFormData addressDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    addressDetails = buildAddressFormData();
    errors = new BeanPropertyBindingResult(addressDetails, "addressDetails");
  }

  @Test
  public void supports_ReturnsTrueForAddressFormDataClass() {
    assertTrue(correspondenceAddressValidator.supports(AddressFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(correspondenceAddressValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_countryRequired(String country) {
    addressDetails.setCountry(country);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.country", errors.getFieldError("country").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_houseNameNumberRequired(String houseNameNumber) {
    addressDetails.setHouseNameNumber(houseNameNumber);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("houseNameNumber"));
    assertEquals("required.houseNameNumber", errors.getFieldError("houseNameNumber").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_addressLine1Required(String addressLine1) {
    addressDetails.setAddressLine1(addressLine1);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("addressLine1"));
    assertEquals("required.addressLine1", errors.getFieldError("addressLine1").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_cityTownRequired(String cityTown) {
    addressDetails.setCityTown(cityTown);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("cityTown"));
    assertEquals("required.cityTown", errors.getFieldError("cityTown").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "USA",
      "GBR"
  })
  public void validate_validatePostcodeFormat(String country) {
    addressDetails.setCountry(country);
    addressDetails.setPostcode("@@@@@@");
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("invalid.format", errors.getFieldError("postcode").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  private AddressFormData buildAddressFormData() {
    AddressFormData addressDetails = new AddressFormData();
    addressDetails.setPreferredAddress("CASE");
    addressDetails.setCountry("GBR");
    addressDetails.setHouseNameNumber("1234");
    addressDetails.setPostcode("SW1A 1AA");
    addressDetails.setAddressLine1("Address Line 1");
    addressDetails.setCityTown("CITY");
    return addressDetails;
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "12  The  Street",//Double Spaces
      "12@The :Street"//Invalid Characters
  })
  void validate_InvalidAddressLine1Format(String addressLine1) {
    addressDetails.setAddressLine1(addressLine1);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "12  The  Street",//Double Spaces
      "12@The :Street"//Invalid Characters
  })
  void validate_InvalidAddressLine2Format(String addressLine2) {
    addressDetails.setAddressLine2(addressLine2);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "South  Cardiff",//Double Spaces
      "North:@Swansea;"//Invalid Characters
  })
  void validate_InvalidCityTownFormat(String cityTown) {
    addressDetails.setCityTown(cityTown);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "Vale  of  Glamorgan",//Double Spaces
      "Vale;of@Glamorgan:"//Invalid Characters
  })
  void validate_InvalidCountyFormat(String county) {
    addressDetails.setCounty(county);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }
}
