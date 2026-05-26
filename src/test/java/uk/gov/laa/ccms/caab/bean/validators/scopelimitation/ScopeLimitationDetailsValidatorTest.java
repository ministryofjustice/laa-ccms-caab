package uk.gov.laa.ccms.caab.bean.validators.scopelimitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

@ExtendWith(MockitoExtension.class)
class ScopeLimitationDetailsValidatorTest {

  @InjectMocks private ScopeLimitationDetailsValidator scopeLimitationDetailsValidator;

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

  @Test
  public void validateScopeLimitationWording_WithMaxLengthExceeded_HasErrors() {
    final ScopeLimitationDetail scopeLimitation =
        new ScopeLimitationDetail().scopeLimitationWording("A".repeat(951));
    final Errors scopeLimitationErrors =
        new BeanPropertyBindingResult(scopeLimitation, "scopeLimitation");

    scopeLimitationDetailsValidator.validateScopeLimitationWording(
        scopeLimitation, scopeLimitationErrors);

    assertTrue(scopeLimitationErrors.hasErrors());
    assertNotNull(scopeLimitationErrors.getFieldError("scopeLimitationWording"));
    assertEquals(
        "length.exceeds.max",
        scopeLimitationErrors.getFieldError("scopeLimitationWording").getCode());
  }

  @Test
  public void validateScopeLimitationWording_WithMaxLength_NoErrors() {
    final ScopeLimitationDetail scopeLimitation =
        new ScopeLimitationDetail().scopeLimitationWording("A".repeat(950));
    final Errors scopeLimitationErrors =
        new BeanPropertyBindingResult(scopeLimitation, "scopeLimitation");

    scopeLimitationDetailsValidator.validateScopeLimitationWording(
        scopeLimitation, scopeLimitationErrors);

    assertFalse(scopeLimitationErrors.hasErrors());
  }
}
