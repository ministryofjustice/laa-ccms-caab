package uk.gov.laa.ccms.caab.bean.validators.scopelimitation;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

/** Validates the scope limitation details provided by scope limitation flow forms. */
@Component
public class ScopeLimitationDetailsValidator extends AbstractValidator {

  private static final int SCOPE_LIMITATION_WORDING_MAX_LENGTH = 950;

  private static final String SCOPE_LIMITATION_WORDING_FIELD = "scopeLimitationWording";

  private static final String SCOPE_LIMITATION_WORDING_DISPLAY = "Scope limitation wording";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails}, {@code false}
   *     otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ScopeLimitationFormDataDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the scope limitation details in the {@link
   * uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ScopeLimitationFormDataDetails scopeLimitationDetails =
        (ScopeLimitationFormDataDetails) target;

    validateRequiredField(
        "scopeLimitation", scopeLimitationDetails.getScopeLimitation(), "Scope limitation", errors);
  }

  /**
   * Validates editable scope limitation wording.
   *
   * <p>The wording is only editable when EBS flags the scope limitation as requiring non-default
   * wording ({@code nonDefaultWordingReqd == true}). For those, the wording is mandatory and is
   * checked against the standard character set and maximum length, mirroring old PUI behaviour.
   * When the wording is read-only, there is nothing to validate.
   *
   * @param scopeLimitation the scope limitation containing the submitted wording.
   * @param errors The Errors object to store validation errors.
   */
  public void validateScopeLimitationWording(
      final ScopeLimitationDetail scopeLimitation, final Errors errors) {
    if (!Boolean.TRUE.equals(scopeLimitation.getNonDefaultWordingReqd())) {
      return;
    }

    final String wording = scopeLimitation.getScopeLimitationWording();

    validateRequiredField(
        SCOPE_LIMITATION_WORDING_FIELD, wording, SCOPE_LIMITATION_WORDING_DISPLAY, errors);

    if (StringUtils.hasText(wording)) {
      validateFieldFormat(
          SCOPE_LIMITATION_WORDING_FIELD,
          wording,
          STANDARD_CHARACTER_SET,
          SCOPE_LIMITATION_WORDING_DISPLAY,
          errors);
      validateFieldMaxLength(
          SCOPE_LIMITATION_WORDING_FIELD,
          wording,
          SCOPE_LIMITATION_WORDING_MAX_LENGTH,
          SCOPE_LIMITATION_WORDING_DISPLAY,
          errors);
    }
  }
}
