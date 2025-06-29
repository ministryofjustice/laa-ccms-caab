package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataFurtherDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator the further details provided by proceeding flow forms. */
@Component
public class ProceedingFurtherDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData}, {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ProceedingFlowFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the further details in the {@link
   * uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ProceedingFlowFormData formData = (ProceedingFlowFormData) target;
    final ProceedingFormDataFurtherDetails furtherDetails = formData.getFurtherDetails();

    validateRequiredField(
        "clientInvolvementType",
        furtherDetails.getClientInvolvementType(),
        "Client involvement type",
        errors);

    validateRequiredField(
        "levelOfService",
        furtherDetails.getLevelOfService(),
        "Form of Civil Legal Service",
        errors);

    if (Boolean.TRUE.equals(formData.getProceedingDetails().getOrderTypeRequired())) {
      validateRequiredField(
          "typeOfOrder", furtherDetails.getTypeOfOrder(), "Type of order", errors);
    }
  }
}
