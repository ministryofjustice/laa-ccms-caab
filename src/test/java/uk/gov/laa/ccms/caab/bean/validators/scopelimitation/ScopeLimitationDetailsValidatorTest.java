package uk.gov.laa.ccms.caab.bean.validators.scopelimitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails;

@ExtendWith(SpringExtension.class)
class ScopeLimitationDetailsValidatorTest {

  @InjectMocks
  private ScopeLimitationDetailsValidator scopeLimitationDetailsValidator;

  private ScopeLimitationFormDataDetails scopeLimitationDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    scopeLimitationDetails = new ScopeLimitationFormDataDetails();
    errors = new BeanPropertyBindingResult(scopeLimitationDetails, "scopeLimitationDetails");
  }

  @Test
  public void supports_ReturnsTrueForScopeLimitationFormDataDetailsClass() {
    assertTrue(scopeLimitationDetailsValidator.supports(ScopeLimitationFormDataDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(scopeLimitationDetailsValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullScopeLimitation_HasErrors() {
    scopeLimitationDetails.setScopeLimitation(null);
    scopeLimitationDetailsValidator.validate(scopeLimitationDetails, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("scopeLimitation"));
    assertEquals("required.scopeLimitation", errors.getFieldError("scopeLimitation").getCode());
  }

  @Test
  public void validate_WithValidScopeLimitation_NoErrors() {
    scopeLimitationDetails.setScopeLimitation("Valid Scope Limitation");
    scopeLimitationDetailsValidator.validate(scopeLimitationDetails, errors);
    assertFalse(errors.hasErrors());
  }

}
