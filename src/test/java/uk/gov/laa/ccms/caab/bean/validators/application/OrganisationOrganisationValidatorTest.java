package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;

@ExtendWith(SpringExtension.class)
class OrganisationOrganisationValidatorTest {

  @InjectMocks
  private OpponentOrganisationValidator validator;

  private OpponentFormData opponentFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    opponentFormData =
        new OpponentFormData(); // Assuming that the default constructor sets all fields to null.
    opponentFormData.setOtherInformation(StringUtils.repeat("a", 2001));
    errors = new BeanPropertyBindingResult(opponentFormData, CURRENT_OPPONENT);
  }

  @Test
  public void supports_ReturnsTrueForCorrectClass() {
    assertTrue(validator.supports(OpponentFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate() {
    validator.validate(opponentFormData, errors);
    assertTrue(errors.hasErrors());

    assertNotNull(errors.getFieldError("relationshipToCase"));
    assertEquals("required.relationshipToCase", errors.getFieldError("relationshipToCase").getCode());

    assertNotNull(errors.getFieldError("relationshipToClient"));
    assertEquals("required.relationshipToCase", errors.getFieldError("relationshipToCase").getCode());

    assertNotNull(errors.getFieldError("otherInformation"));
    assertEquals("length.exceeds.max", errors.getFieldError("otherInformation").getCode());
  }

}