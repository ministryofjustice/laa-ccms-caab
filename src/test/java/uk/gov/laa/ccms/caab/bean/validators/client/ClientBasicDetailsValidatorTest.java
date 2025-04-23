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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;

@ExtendWith(SpringExtension.class)
class ClientBasicDetailsValidatorTest {

  @InjectMocks
  private ClientBasicDetailsValidator clientBasicDetailsValidator;

  private ClientFormDataBasicDetails basicDetails;

  private Errors errors;

  @BeforeEach
  void setUp() {
    basicDetails = new ClientFormDataBasicDetails();
    errors = new BeanPropertyBindingResult(basicDetails, "basicDetails");
  }

  @Test
  void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(clientBasicDetailsValidator.supports(ClientFormDataBasicDetails.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientBasicDetailsValidator.supports(Object.class));
  }

  @Test
  void validate() {
    basicDetails = buildBasicDetails();
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_titleRequired(String title) {
    basicDetails = buildBasicDetails();
    basicDetails.setTitle(title);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("title"));
    assertEquals("required.title", errors.getFieldError("title").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_surnameRequired(String surname) {
    basicDetails = buildBasicDetails();
    basicDetails.setSurname(surname);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("surname"));
    assertEquals("required.surname", errors.getFieldError("surname").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_countryOfOriginRequired(String countryOfOrigin) {
    basicDetails = buildBasicDetails();
    basicDetails.setCountryOfOrigin(countryOfOrigin);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("countryOfOrigin"));
    assertEquals("required.countryOfOrigin", errors.getFieldError("countryOfOrigin").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_genderRequired(String gender) {
    basicDetails = buildBasicDetails();
    basicDetails.setGender(gender);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("gender"));
    assertEquals("required.gender", errors.getFieldError("gender").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_maritalStatusRequired(String maritalStatus) {
    basicDetails = buildBasicDetails();
    basicDetails.setMaritalStatus(maritalStatus);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("maritalStatus"));
    assertEquals("required.maritalStatus", errors.getFieldError("maritalStatus").getCode());
  }

  @ParameterizedTest
  @CsvSource({"a  b",
      "1A",
      "A1"})
  void validateInvalidMiddleNames(String middleNames) {
    basicDetails = buildBasicDetails();
    basicDetails.setMiddleNames(middleNames);
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @ParameterizedTest
  @CsvSource({"a  b",
      "1A",
      "A1"})
  void validateInvalidSurname(String surname) {
    basicDetails = buildBasicDetails();
    basicDetails.setSurname(surname);
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateInvalidNationalInsuranceNumber() {
    basicDetails = buildBasicDetails();
    basicDetails.setNationalInsuranceNumber("ABC123");
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateValidNationalInsuranceNumber() {
    basicDetails = buildBasicDetails();
    basicDetails.setNationalInsuranceNumber("AA100000A");
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  void validateInvalidHomeOfficeNumber() {
    basicDetails = buildBasicDetails();
    basicDetails.setHomeOfficeNumber("1 $2 AS");
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateValidHomeOfficeNumber() {
    basicDetails = buildBasicDetails();
    basicDetails.setHomeOfficeNumber("AA12356");
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  private ClientFormDataBasicDetails buildBasicDetails() {
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setTitle("MR");
    basicDetails.setSurname("TEST");
    basicDetails.setCountryOfOrigin("UK");
    basicDetails.setGender("MALE");
    basicDetails.setMaritalStatus("SINGLE");
    return basicDetails;
  }

}
