package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator the details provided by client equal opportunities monitoring form. */
@Component
public class ClientEqualOpportunitiesMonitoringDetailsValidator extends AbstractValidator {

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
  }
}
