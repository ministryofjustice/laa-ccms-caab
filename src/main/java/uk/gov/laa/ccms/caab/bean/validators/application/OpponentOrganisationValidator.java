package uk.gov.laa.ccms.caab.bean.validators.application;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validates the organisation opponent details provided by the user.
 */
@Component
public class OpponentOrganisationValidator extends AbstractValidator {

  /**
   * Determine if this validator can validate instances of the given class.
   *
   * @param clazz the class to check
   * @return whether this validator can validate instances of the given class.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return OpponentFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(Object target, Errors errors) {
    OpponentFormData opponentFormData = (OpponentFormData) target;
    validateRequiredField("relationshipToCase", opponentFormData.getRelationshipToCase(),
        "Relationship to case", errors);
    validateRequiredField("relationshipToClient", opponentFormData.getRelationshipToClient(),
        "Relationship to client", errors);
    validateFieldMaxLength("otherInformation", opponentFormData.getOtherInformation(),
        2000, "Other information", errors);
  }
}
