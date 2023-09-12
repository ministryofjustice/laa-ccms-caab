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
import uk.gov.laa.ccms.caab.bean.ClientDetails;

@ExtendWith(SpringExtension.class)
class ClientBasicDetailsValidatorTest {

  @InjectMocks
  private ClientBasicDetailsValidator clientBasicDetailsValidator;

  private ClientDetails clientDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    clientDetails = new ClientDetails();
    errors = new BeanPropertyBindingResult(clientDetails, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(clientBasicDetailsValidator.supports(ClientDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientBasicDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    clientDetails = buildClientDetails();
    clientBasicDetailsValidator.validate(clientDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_titleRequired(String title) {
    clientDetails = buildClientDetails();
    clientDetails.setTitle(title);

    clientBasicDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("title"));
    assertEquals("required.title", errors.getFieldError("title").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_surnameRequired(String surname) {
    clientDetails = buildClientDetails();
    clientDetails.setSurname(surname);

    clientBasicDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("surname"));
    assertEquals("required.surname", errors.getFieldError("surname").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_countryOfOriginRequired(String countryOfOrigin) {
    clientDetails = buildClientDetails();
    clientDetails.setCountryOfOrigin(countryOfOrigin);

    clientBasicDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("countryOfOrigin"));
    assertEquals("required.countryOfOrigin", errors.getFieldError("countryOfOrigin").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_genderRequired(String gender) {
    clientDetails = buildClientDetails();
    clientDetails.setGender(gender);

    clientBasicDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("gender"));
    assertEquals("required.gender", errors.getFieldError("gender").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_maritalStatusRequired(String maritalStatus) {
    clientDetails = buildClientDetails();
    clientDetails.setMaritalStatus(maritalStatus);

    clientBasicDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("maritalStatus"));
    assertEquals("required.maritalStatus", errors.getFieldError("maritalStatus").getCode());
  }

  private ClientDetails buildClientDetails(){
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setTitle("MR");
    clientDetails.setSurname("TEST");
    clientDetails.setCountryOfOrigin("UK");
    clientDetails.setGender("MALE");
    clientDetails.setMaritalStatus("SINGLE");
    return clientDetails;
  }

}