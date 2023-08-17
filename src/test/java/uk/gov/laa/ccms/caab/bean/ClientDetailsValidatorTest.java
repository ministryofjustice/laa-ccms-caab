package uk.gov.laa.ccms.caab.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

@ExtendWith(SpringExtension.class)
class ClientDetailsValidatorTest {

  private ClientDetailsValidator validator;

  @Mock
  private ClientDetails clientDetails;

  @Mock
  private Errors errors;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    validator = new ClientDetailsValidator();
  }

  @Test
  void testSupports() {
    assertTrue(validator.supports(ClientDetails.class));
    assertFalse(validator.supports(String.class));
  }

  @Test
  void testValidateTitle() {
    // Simulate title is empty
    when(clientDetails.getTitle()).thenReturn("");
    validator.validateTitle(clientDetails, errors);
    verify(errors).rejectValue("title", "required.title", null,
        "Please complete 'Title'.");
  }

  @Test
  void testValidateSurname() {
    // Simulate surname is empty
    when(clientDetails.getSurname()).thenReturn("");
    validator.validateSurname(clientDetails, errors);
    verify(errors).rejectValue("surname", "required.surname", null,
        "Please complete 'Surname'.");
  }

  @Test
  void testValidateCountryOfOrigin() {
    // Simulate countryOfOrigin is empty
    when(clientDetails.getCountryOfOrigin()).thenReturn("");
    validator.validateCountryOfOrigin(clientDetails, errors);
    verify(errors).rejectValue("countryOfOrigin", "required.countryOfOrigin", null,
        "Please complete 'Country of origin'.");
  }

  @Test
  void testValidateGender() {
    // Simulate gender is empty
    when(clientDetails.getGender()).thenReturn("");
    validator.validateGender(clientDetails, errors);
    verify(errors).rejectValue("gender", "required.gender", null,
        "Please complete 'Gender'.");
  }

  @Test
  void testValidateMaritalStatus() {
    // Simulate maritalStatus is empty
    when(clientDetails.getMaritalStatus()).thenReturn("");
    validator.validateMaritalStatus(clientDetails, errors);
    verify(errors).rejectValue("maritalStatus", "required.maritalStatus", null,
        "Please complete 'Marital status'.");
  }

  @Test
  void testValidateAllFields() {
    // You can further mock scenarios where all the fields are either valid or invalid
    // and then check if the validator's validate() method works as expected.
    // This can include cases where all fields are empty, where all fields have valid values, and a mix of both.

    // Here's a simple example:
    BindException errors = new BindException(clientDetails, "clientDetails");
    validator.validate(clientDetails, errors);

    // In this example, if all fields are empty, we would expect 5 errors.
    assertEquals(5, errors.getErrorCount());
  }

}