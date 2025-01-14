package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;

@ExtendWith(MockitoExtension.class)
class ClientDeceasedDetailsValidatorTest {

  private ClientFormDataDeceasedDetails deceasedDetails;

  @InjectMocks
  private ClientDeceasedDetailsValidator validator;

  private Errors errors;

  @BeforeEach
  void setup() {
    deceasedDetails = new ClientFormDataDeceasedDetails();
    errors = new BeanPropertyBindingResult(deceasedDetails, "deceasedDetails");
  }

  @Test
  void supports_ReturnsTrueForClientFormDataDeceasedDetailsClass() {
    assertTrue(validator.supports(ClientFormDataDeceasedDetails.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/01/2000",
      "01//2000",
      "01/01/"})
  void testValidate_numericFieldsError(String dodDay) {
    deceasedDetails.setDateOfDeath(dodDay);

    validator.validate(deceasedDetails, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dateOfDeath"));
    assertEquals("invalid.format", errors.getFieldError("dateOfDeath").getCode());
  }

  @Test
  void testValidate_futureDateError() {
    deceasedDetails.setDateOfDeath("1/1/3000");

    validator.validate(deceasedDetails, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dateOfDeath"));
    assertEquals("invalid.input", errors.getFieldError("dateOfDeath").getCode());
  }

  @Test
  void testValidate() {
    deceasedDetails.setDateOfDeath("01/01/2000");

    validator.validate(deceasedDetails, errors);

    assertFalse(errors.hasErrors());
  }


}