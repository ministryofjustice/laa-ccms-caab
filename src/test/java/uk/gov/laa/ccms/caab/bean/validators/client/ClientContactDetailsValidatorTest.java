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
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;

@ExtendWith(SpringExtension.class)
class ClientContactDetailsValidatorTest {

  @InjectMocks
  private ClientContactDetailsValidator clientContactDetailsValidator;

  @Mock
  private ClientFormDataContactDetails contactDetails;

  private Errors errors;

  @BeforeEach
  void setUp() {
    contactDetails = buildContactDetails();
    errors = new BeanPropertyBindingResult(contactDetails, "contactDetails");
  }

  @Test
  void supports_ReturnsTrueForClientFormDataContactDetailsClass() {
    assertTrue(clientContactDetailsValidator.supports(ClientFormDataContactDetails.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(clientContactDetailsValidator.supports(Object.class));
  }

  @Test
  void validate() {
    clientContactDetailsValidator.validate(contactDetails, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_passwordRequired(String password) {
    contactDetails.setPassword(password);

    clientContactDetailsValidator.validate(contactDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("password"));
    assertEquals("required.password", errors.getFieldError("password").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_passwordReminderRequired(String passwordReminder) {
    contactDetails.setPasswordReminder(passwordReminder);

    clientContactDetailsValidator.validate(contactDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("passwordReminder"));
    assertEquals("required.passwordReminder", errors.getFieldError("passwordReminder").getCode());
  }

  @Test
  void validate_validatePasswordNeedsReminder() {
    contactDetails.setPassword("Test");
    contactDetails.setPasswordReminder("Test");

    clientContactDetailsValidator.validate(contactDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("password"));
    assertEquals("same.passwordReminder", errors.getFieldError("password").getCode());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_validateEmailField(String emailAddress) {
    contactDetails.setEmailAddress(emailAddress);
    contactDetails.setCorrespondenceMethod("E-mail");

    clientContactDetailsValidator.validate(contactDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("emailAddress"));
    assertEquals("required.emailAddress", errors.getFieldError("emailAddress").getCode());
  }

  @Test
  void validateInvalidEmailAddress() {
    contactDetails.setEmailAddress("upanddown.mm");
    clientContactDetailsValidator.validate(contactDetails, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validateValidEmailAddress() {
    contactDetails.setEmailAddress("harrysmith@gmail.com");
    clientContactDetailsValidator.validate(contactDetails, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_validateTelephones(String telephone) {
    contactDetails.setVulnerableClient(false);
    contactDetails.setTelephoneHome(telephone);
    contactDetails.setTelephoneWork(telephone);
    contactDetails.setTelephoneMobile(telephone);

    clientContactDetailsValidator.validate(contactDetails, errors);
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
  void validate_validateTelephoneField_invalidCharacters(String type, String telephone) {
    contactDetails.setVulnerableClient(false);

    if (type.equals("telephoneHome"))
      contactDetails.setTelephoneHome(telephone);
    if (type.equals("telephoneWork"))
      contactDetails.setTelephoneWork(telephone);
    if (type.equals("telephoneMobile"))
      contactDetails.setTelephoneMobile(telephone);

    clientContactDetailsValidator.validate(contactDetails, errors);
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
  void validate_validateTelephoneField_invalidLength(String type, String telephone) {
    contactDetails.setVulnerableClient(false);

    if (type.equals("telephoneHome"))
      contactDetails.setTelephoneHome(telephone);
    if (type.equals("telephoneWork"))
      contactDetails.setTelephoneWork(telephone);
    if (type.equals("telephoneMobile"))
      contactDetails.setTelephoneMobile(telephone);

    clientContactDetailsValidator.validate(contactDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError(type));
    assertEquals("length." + type, errors.getFieldError(type).getCode());
  }


  private ClientFormDataContactDetails buildContactDetails(){
    ClientFormDataContactDetails contactDetails1 = new ClientFormDataContactDetails();
    contactDetails1.setTelephoneHome("1111111111");
    contactDetails1.setTelephoneWork("2222222222");
    contactDetails1.setTelephoneMobile("3333333333");
    contactDetails1.setTelephoneHomePresent(true);
    contactDetails1.setTelephoneWorkPresent(true);
    contactDetails1.setTelephoneMobilePresent(true);
    contactDetails1.setEmailAddress("test@test.com");
    contactDetails1.setPassword("password");
    contactDetails1.setPasswordReminder("reminder");
    contactDetails1.setCorrespondenceMethod("LETTER");
    contactDetails1.setCorrespondenceLanguage("GBR");
    return contactDetails1;
  }




}