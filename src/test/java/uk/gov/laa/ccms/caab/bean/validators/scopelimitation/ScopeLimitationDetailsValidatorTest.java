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
        new ScopeLimitationDetail()
            .nonDefaultWordingReqd(true)
            .scopeLimitationWording("A".repeat(951));
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
        new ScopeLimitationDetail()
            .nonDefaultWordingReqd(true)
            .scopeLimitationWording("A".repeat(950));
    final Errors scopeLimitationErrors =
        new BeanPropertyBindingResult(scopeLimitation, "scopeLimitation");

    scopeLimitationDetailsValidator.validateScopeLimitationWording(
        scopeLimitation, scopeLimitationErrors);

    assertFalse(scopeLimitationErrors.hasErrors());
  }

  @Test
  public void validateScopeLimitationWording_WhenRequiredAndBlank_HasRequiredError() {
    final ScopeLimitationDetail scopeLimitation =
        new ScopeLimitationDetail().nonDefaultWordingReqd(true).scopeLimitationWording("  ");
    final Errors scopeLimitationErrors =
        new BeanPropertyBindingResult(scopeLimitation, "scopeLimitation");

    scopeLimitationDetailsValidator.validateScopeLimitationWording(
        scopeLimitation, scopeLimitationErrors);

    assertTrue(scopeLimitationErrors.hasErrors());
    assertEquals(
        "required.scopeLimitationWording",
        scopeLimitationErrors.getFieldError("scopeLimitationWording").getCode());
  }

  @Test
  public void validateScopeLimitationWording_WhenRequiredAndInvalidCharacter_HasFormatError() {
    final ScopeLimitationDetail scopeLimitation =
        new ScopeLimitationDetail()
            .nonDefaultWordingReqd(true)
            .scopeLimitationWording("Invalid wording ☃");
    final Errors scopeLimitationErrors =
        new BeanPropertyBindingResult(scopeLimitation, "scopeLimitation");

    scopeLimitationDetailsValidator.validateScopeLimitationWording(
        scopeLimitation, scopeLimitationErrors);

    assertTrue(scopeLimitationErrors.hasErrors());
    assertEquals(
        "invalid.format", scopeLimitationErrors.getFieldError("scopeLimitationWording").getCode());
  }

  @Test
  public void validateScopeLimitationWording_WhenNotEditable_SkipsValidation() {
    // Read-only wording (nonDefaultWordingReqd not true) must not be validated even when blank or
    // over length, because the user cannot change it.
    final ScopeLimitationDetail scopeLimitation =
        new ScopeLimitationDetail()
            .nonDefaultWordingReqd(false)
            .scopeLimitationWording("A".repeat(951));
    final Errors scopeLimitationErrors =
        new BeanPropertyBindingResult(scopeLimitation, "scopeLimitation");

    scopeLimitationDetailsValidator.validateScopeLimitationWording(
        scopeLimitation, scopeLimitationErrors);

    assertFalse(scopeLimitationErrors.hasErrors());
  }
}
