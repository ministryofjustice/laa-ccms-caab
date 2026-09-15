package uk.gov.laa.ccms.caab.bean.validators.client;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.SPECIAL_CONSIDERATIONS_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator the details provided by client equal opportunities monitoring form. */
@Component
public class ClientEqualOpportunitiesMonitoringDetailsValidator extends AbstractValidator {

  private static final String SPECIAL_CONSIDERATIONS_DISPLAY_NAME = "Special considerations";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataMonitoringDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client equal opportunities monitoring details in the {@link
   * uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientFormDataMonitoringDetails monitoringDetails = (ClientFormDataMonitoringDetails) target;

    validateRequiredField(
        "ethnicOrigin", monitoringDetails.getEthnicOrigin(), "Ethnic monitoring", errors);
    validateRequiredField(
        "disability", monitoringDetails.getDisability(), "Disability monitoring", errors);

    if (StringUtils.hasText(monitoringDetails.getSpecialConsiderations())) {
      validateFieldFormat(
          "specialConsiderations",
          monitoringDetails.getSpecialConsiderations(),
          STANDARD_CHARACTER_SET,
          SPECIAL_CONSIDERATIONS_DISPLAY_NAME,
          errors);
      validateFieldMaxLength(
          "specialConsiderations",
          monitoringDetails.getSpecialConsiderations(),
          SPECIAL_CONSIDERATIONS_CHARACTER_SIZE,
          SPECIAL_CONSIDERATIONS_DISPLAY_NAME,
          errors);
    }
  }
}
