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
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;

@ExtendWith(SpringExtension.class)
class ClientDeceasedDetailsValidatorTest {

  private ClientFormDataDeceasedDetails deceasedDetails;

  @InjectMocks
  private ClientDeceasedDetailsValidator validator;

  private Errors errors;

  @BeforeEach
  public void setup() {
    deceasedDetails = new ClientFormDataDeceasedDetails();
    errors = new BeanPropertyBindingResult(deceasedDetails, "deceasedDetails");
  }

  @Test
  public void supports_ReturnsTrueForClientFormDataDeceasedDetailsClass() {
    assertTrue(validator.supports(ClientFormDataDeceasedDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @ParameterizedTest
  @CsvSource({",01,2000,dodDay",
      "01,,2000,dodMonth",
      "01,01,,dodYear"})
  public void testValidate_numericFieldsError(String dodDay, String dodMonth, String dodYear, String erroredField){
    deceasedDetails.setDodDay(dodDay);
    deceasedDetails.setDodMonth(dodMonth);
    deceasedDetails.setDodYear(dodYear);

    validator.validate(deceasedDetails, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError(erroredField));
    assertEquals("invalid.numeric", errors.getFieldError(erroredField).getCode());
  }

  @Test
  public void testValidate_futureDateError(){
    deceasedDetails.setDodDay("1");
    deceasedDetails.setDodMonth("1");
    deceasedDetails.setDodYear("3000");

    validator.validate(deceasedDetails, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("dateOfDeath"));
    assertEquals("invalid.input", errors.getFieldError("dateOfDeath").getCode());
  }

  @Test
  public void testValidate(){
    deceasedDetails.setDodDay("01");
    deceasedDetails.setDodMonth("01");
    deceasedDetails.setDodYear("2000");

    validator.validate(deceasedDetails, errors);

    assertFalse(errors.hasErrors());
  }



}