package uk.gov.laa.ccms.caab.bean.validators.declaration;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator for the POA declaration: <em>every</em> declaration statement must be acknowledged
 * before the POA can be submitted.
 *
 * <p>This mirrors the legacy PUI's CCMS_DE01 rule ({@code de01_all_checkboxes_checked} / {@code
 * Declaration.isCheckboxesNotChecked}), which blocks the submit unless all checkboxes are ticked -
 * the bill and POA submissions share that rule. It is deliberately stricter than the shared {@link
 * DeclarationSubmissionValidator}, which requires only one box and matches the separate means
 * reassessment declaration.
 *
 * <p>The declaration template binds a hidden field per option so every declaration is present in
 * the submitted data (an unticked checkbox submits nothing on its own), which is what lets this
 * distinguish "all ticked" from "some ticked".
 */
@Component
public class PoaDeclarationSubmissionValidator extends AbstractValidator {

  @Override
  public boolean supports(final Class<?> clazz) {
    return SummarySubmissionFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates that every declaration option has been acknowledged.
   *
   * @param target the {@link SummarySubmissionFormData} to validate.
   * @param errors the errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final SummarySubmissionFormData formData = (SummarySubmissionFormData) target;

    if (formData.getDeclarationOptions() == null
        || formData.getDeclarationOptions().isEmpty()
        || !formData.getDeclarationOptions().stream().allMatch(DynamicCheckbox::isChecked)) {
      errors.reject(
          "declaration.required",
          "You must read and acknowledge the Declaration(s) in order to proceed to submit.");
    }
  }
}
