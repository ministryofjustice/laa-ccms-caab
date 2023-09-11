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
class ClientContactDetailsValidatorTest {

  @InjectMocks
  private ClientContactDetailsValidator clientContactDetailsValidator;

  @Mock
  private ClientDetails clientDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    clientDetails = buildClientDetails();
    errors = new BeanPropertyBindingResult(clientDetails, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(clientContactDetailsValidator.supports(ClientDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientContactDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate() {
    clientContactDetailsValidator.validate(clientDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_passwordRequired(String password) {
    clientDetails.setPassword(password);

    clientContactDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("password"));
    assertEquals("required.password", errors.getFieldError("password").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_passwordReminderRequired(String passwordReminder) {
    clientDetails.setPasswordReminder(passwordReminder);

    clientContactDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("passwordReminder"));
    assertEquals("required.passwordReminder", errors.getFieldError("passwordReminder").getCode());
  }

  @Test
  public void validate_validatePasswordNeedsReminder() {
    clientDetails.setPassword("Test");
    clientDetails.setPasswordReminder("Test");

    clientContactDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("password"));
    assertEquals("same.passwordReminder", errors.getFieldError("password").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_validateEmailField(String emailAddress) {
    clientDetails.setEmailAddress(emailAddress);
    clientDetails.setCorrespondenceMethod("E-mail");

    clientContactDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("emailAddress"));
    assertEquals("required.emailAddress", errors.getFieldError("emailAddress").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  public void validate_validateTelephones(String telephone) {
    clientDetails.setVulnerableClient(false);
    clientDetails.setTelephoneHome(telephone);
    clientDetails.setTelephoneWork(telephone);
    clientDetails.setTelephoneMobile(telephone);

    clientContactDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
  }

  @ParameterizedTest
  @CsvSource({
      "telephoneHome, abc",
      "telephoneHome, @£$",
      "telephoneWork, abc",
      "telephoneWork, @£$",
      "telephoneMobile, abc",
      "telephoneMobile, @£$",
  })
  public void validate_validateTelephoneField_invalidCharacters(String type, String telephone) {
    clientDetails.setVulnerableClient(false);

    if (type.equals("telephoneHome"))
      clientDetails.setTelephoneHome(telephone);
    if (type.equals("telephoneWork"))
      clientDetails.setTelephoneWork(telephone);
    if (type.equals("telephoneMobile"))
      clientDetails.setTelephoneMobile(telephone);

    clientContactDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError(type));
    assertEquals("invalid." + type, errors.getFieldError(type).getCode());
  }

  @ParameterizedTest
  @CsvSource({
      "telephoneHome, 1234567",
      "telephoneHome, 123",
      "telephoneWork, 1234567",
      "telephoneWork, 123",
      "telephoneMobile, 1234567",
      "telephoneMobile, 123",
  })
  public void validate_validateTelephoneField_invalidLength(String type, String telephone) {
    clientDetails.setVulnerableClient(false);

    if (type.equals("telephoneHome"))
      clientDetails.setTelephoneHome(telephone);
    if (type.equals("telephoneWork"))
      clientDetails.setTelephoneWork(telephone);
    if (type.equals("telephoneMobile"))
      clientDetails.setTelephoneMobile(telephone);

    clientContactDetailsValidator.validate(clientDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError(type));
    assertEquals("length." + type, errors.getFieldError(type).getCode());
  }


  private ClientDetails buildClientDetails(){
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setTelephoneHome("1111111111");
    clientDetails.setTelephoneWork("2222222222");
    clientDetails.setTelephoneMobile("3333333333");
    clientDetails.setEmailAddress("test@test.com");
    clientDetails.setPassword("password");
    clientDetails.setPasswordReminder("reminder");
    clientDetails.setCorrespondenceMethod("LETTER");
    clientDetails.setCorrespondenceLanguage("GBR");
    return clientDetails;
  }




}