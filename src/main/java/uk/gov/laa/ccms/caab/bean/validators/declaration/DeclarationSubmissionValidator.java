package uk.gov.laa.ccms.caab.bean.validators.declaration;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator for the declaration details provided by summary submission form data. */
@Component
public class DeclarationSubmissionValidator extends AbstractValidator {

  @Override
  public boolean supports(final Class<?> clazz) {
    return SummarySubmissionFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the declaration details in the {@link
   * uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final SummarySubmissionFormData summarySubmissionFormData = (SummarySubmissionFormData) target;

    // Check if any declarations were selected
    if (summarySubmissionFormData.getDeclarationOptions() == null
        || summarySubmissionFormData.getDeclarationOptions().stream()
            .noneMatch(DynamicCheckbox::isChecked)) {
      errors.reject(
          "declaration.required",
          "You must read and acknowledge the Declaration(s) in order to proceed to submit.");
    }
  }
}
