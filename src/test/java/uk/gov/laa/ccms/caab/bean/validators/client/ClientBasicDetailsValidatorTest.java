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
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

@ExtendWith(SpringExtension.class)
class ClientBasicDetailsValidatorTest {

  @InjectMocks
  private ClientBasicDetailsValidator clientBasicDetailsValidator;

  private ClientFormDataBasicDetails basicDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    basicDetails = new ClientFormDataBasicDetails();
    errors = new BeanPropertyBindingResult(basicDetails, "basicDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(clientBasicDetailsValidator.supports(ClientFormDataBasicDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientBasicDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    basicDetails = buildBasicDetails();
    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_titleRequired(String title) {
    basicDetails = buildBasicDetails();
    basicDetails.setTitle(title);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("title"));
    assertEquals("required.title", errors.getFieldError("title").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_surnameRequired(String surname) {
    basicDetails = buildBasicDetails();
    basicDetails.setSurname(surname);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("surname"));
    assertEquals("required.surname", errors.getFieldError("surname").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_countryOfOriginRequired(String countryOfOrigin) {
    basicDetails = buildBasicDetails();
    basicDetails.setCountryOfOrigin(countryOfOrigin);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("countryOfOrigin"));
    assertEquals("required.countryOfOrigin", errors.getFieldError("countryOfOrigin").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_genderRequired(String gender) {
    basicDetails = buildBasicDetails();
    basicDetails.setGender(gender);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("gender"));
    assertEquals("required.gender", errors.getFieldError("gender").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_maritalStatusRequired(String maritalStatus) {
    basicDetails = buildBasicDetails();
    basicDetails.setMaritalStatus(maritalStatus);

    clientBasicDetailsValidator.validate(basicDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("maritalStatus"));
    assertEquals("required.maritalStatus", errors.getFieldError("maritalStatus").getCode());
  }

  private ClientFormDataBasicDetails buildBasicDetails(){
    ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setTitle("MR");
    basicDetails.setSurname("TEST");
    basicDetails.setCountryOfOrigin("UK");
    basicDetails.setGender("MALE");
    basicDetails.setMaritalStatus("SINGLE");
    return basicDetails;
  }

}