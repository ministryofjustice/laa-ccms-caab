package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AddressFormData;

@ExtendWith(MockitoExtension.class)
class CorrespondenceAddressValidatorTest {

  @InjectMocks private CorrespondenceAddressValidator correspondenceAddressValidator;

  @Mock private AddressFormData addressDetails;

  private Errors errors;

  @BeforeEach
  void setUp() {
    addressDetails = buildAddressFormData();
    errors = new BeanPropertyBindingResult(addressDetails, "addressDetails");
  }

  @Test
  void supports_ReturnsTrueForAddressFormDataClass() {
    assertTrue(correspondenceAddressValidator.supports(AddressFormData.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(correspondenceAddressValidator.supports(Object.class));
  }

  @Test
  void validate() {
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Returns false if the Country is null")
  void validate_countryRequired(String country) {
    addressDetails.setCountry(country);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("country"));
    assertEquals("required.country", errors.getFieldError("country").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Returns false if the House name / number is null")
  void validate_houseNameNumberRequired(String houseNameNumber) {
    addressDetails.setHouseNameNumber(houseNameNumber);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("houseNameNumber"));
    assertEquals("required.houseNameNumber", errors.getFieldError("houseNameNumber").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Returns false if Address Line 1 is null")
  void validate_addressLine1Required(String addressLine1) {
    addressDetails.setAddressLine1(addressLine1);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("addressLine1"));
    assertEquals("required.addressLine1", errors.getFieldError("addressLine1").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Returns false if the City / town is null")
  void validate_cityTownRequired(String cityTown) {
    addressDetails.setCityTown(cityTown);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("cityTown"));
    assertEquals("required.cityTown", errors.getFieldError("cityTown").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {"USA", "GBR"})
  @DisplayName("Returns false if the Postcode format is invalid")
  void validate_validatePostcodeFormat(String country) {
    addressDetails.setCountry(country);
    addressDetails.setPostcode("@@@@@@");
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("invalid.format", errors.getFieldError("postcode").getCode());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  @DisplayName("Returns required manual address errors when manual details are partially entered")
  void validate_manualAddressFieldsRequiredWhenManualDetailsPartiallyEntered() {
    addressDetails.setPreferredAddress("HOME");
    addressDetails.setCountry("GBR");
    addressDetails.setHouseNameNumber("1234");
    addressDetails.setPostcode(null);
    addressDetails.setAddressLine1(null);
    addressDetails.setCityTown(null);

    correspondenceAddressValidator.validate(addressDetails, errors);

    assertTrue(errors.hasErrors());
    assertEquals(3, errors.getErrorCount());
    assertEquals(
        List.of("required.postcode", "required.addressLine1", "required.cityTown"),
        errors.getFieldErrors().stream().map(error -> error.getCode()).toList());
  }

  @Test
  @DisplayName("Returns postcode required when country is blank for incomplete manual addresses")
  void validate_postcodeRequiredWhenCountryBlankForIncompleteManualAddress() {
    addressDetails.setPreferredAddress("HOME");
    addressDetails.setCountry(null);
    addressDetails.setHouseNameNumber("1234");
    addressDetails.setPostcode(null);
    addressDetails.setAddressLine1(null);
    addressDetails.setCityTown(null);

    correspondenceAddressValidator.validate(addressDetails, errors);

    assertTrue(errors.hasErrors());
    assertEquals(4, errors.getErrorCount());
    assertEquals(
        List.of(
            "required.country", "required.postcode", "required.addressLine1", "required.cityTown"),
        errors.getFieldErrors().stream().map(error -> error.getCode()).toList());
  }

  @Test
  @DisplayName("Returns a single postcode required error when postcode is blank")
  void validate_postcodeRequiredOnce() {
    addressDetails.setPostcode(null);

    correspondenceAddressValidator.validate(addressDetails, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("postcode"));
    assertEquals("required.postcode", errors.getFieldError("postcode").getCode());
    assertEquals(1, errors.getFieldErrors("postcode").size());
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
  @ValueSource(
      strings = {
        "12  The  Street", // Double Spaces
        "12@The :Street" // Invalid Characters
      })
  @DisplayName("Returns false if Address line 1 has invalid characters or double spaces")
  void validate_InvalidAddressLine1Format(String addressLine1) {
    addressDetails.setAddressLine1(addressLine1);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "12  The  Street", // Double Spaces
        "12@The :Street" // Invalid Characters
      })
  @DisplayName("Returns false if Address line 2 has invalid characters or double spaces")
  void validate_InvalidAddressLine2Format(String addressLine2) {
    addressDetails.setAddressLine2(addressLine2);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "South  Cardiff", // Double Spaces
        "North:@Swansea;" // Invalid Characters
      })
  @DisplayName("Returns false if City / town has invalid characters or double spaces")
  void validate_InvalidCityTownFormat(String cityTown) {
    addressDetails.setCityTown(cityTown);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Vale  of  Glamorgan", // Double Spaces
        "Vale;of@Glamorgan:" // Invalid Characters
      })
  @DisplayName("Returns false if the County has invalid characters or double spaces")
  void validate_InvalidCountyFormat(String county) {
    addressDetails.setCounty(county);
    correspondenceAddressValidator.validate(addressDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }
}
