package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;

@ExtendWith(SpringExtension.class)
class PrivacyNoticeAgreementValidatorTest {

  @InjectMocks
  private PrivacyNoticeAgreementValidator privacyNoticeAgreementValidator;

  private ApplicationFormData applicationFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    applicationFormData =
        new ApplicationFormData(); // Assuming that the default constructor sets all fields to null.
    errors = new BeanPropertyBindingResult(applicationFormData, APPLICATION_FORM_DATA);
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(privacyNoticeAgreementValidator.supports(ApplicationFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(privacyNoticeAgreementValidator.supports(Object.class));
  }

  @Test
  public void validate_false() {
    privacyNoticeAgreementValidator.validate(applicationFormData, errors);
    assertTrue(errors.hasErrors());
    assertTrue(errors.hasFieldErrors("agreementAccepted"));
    assertEquals("agreement.not.accepted", errors.getFieldError("agreementAccepted").getCode());
  }

  @Test
  public void validate_true() {
    applicationFormData.setAgreementAccepted(true);
    privacyNoticeAgreementValidator.validate(applicationFormData, errors);
    assertFalse(errors.hasErrors());
  }

}