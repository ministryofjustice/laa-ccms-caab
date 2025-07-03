package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.model.LinkedCaseResultRowDisplay;

/** Validates the linked case form data. */
@Component
public class LinkedCaseValidator extends AbstractValidator {

  /**
   * Determine if this validator can validate instances of the given class.
   *
   * @param clazz the class to check
   * @return whether this validator can validate instances of the given class.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return LinkedCaseResultRowDisplay.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final LinkedCaseResultRowDisplay data = (LinkedCaseResultRowDisplay) target;
    validateRequiredField(
        "relationToCase",
        data.getRelationToCase(),
        "How is this application / case related to your application?",
        errors);
  }
}
