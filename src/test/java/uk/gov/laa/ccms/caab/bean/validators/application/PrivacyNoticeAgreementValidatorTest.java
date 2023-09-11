package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;

@ExtendWith(SpringExtension.class)
class PrivacyNoticeAgreementValidatorTest {

  @InjectMocks
  private PrivacyNoticeAgreementValidator privacyNoticeAgreementValidator;

  private ApplicationDetails applicationDetails;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    applicationDetails =
        new ApplicationDetails(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(applicationDetails, "applicationDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(privacyNoticeAgreementValidator.supports(ApplicationDetails.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(privacyNoticeAgreementValidator.supports(Object.class));
  }

  @Test
  public void validate_false() {
    privacyNoticeAgreementValidator.validate(applicationDetails, errors);
    assertTrue(errors.hasErrors());
    assertTrue(errors.hasFieldErrors("agreementAccepted"));
    assertEquals("agreement.not.accepted", errors.getFieldError("agreementAccepted").getCode());
  }

  @Test
  public void validate_true() {
    applicationDetails.setAgreementAccepted(true);
    privacyNoticeAgreementValidator.validate(applicationDetails, errors);
    assertFalse(errors.hasErrors());
  }

}