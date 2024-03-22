package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;

/**
 * Validates the organisation opponent details provided by the user.
 */
@Component
public class OrganisationOpponentValidator extends AbstractOpponentValidator {

  /**
   * Determine if this validator can validate instances of the given class.
   *
   * @param clazz the class to check
   * @return whether this validator can validate instances of the given class.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return OrganisationOpponentFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the given target object.
   *
   * @param target the target object to validate
   * @param errors contextual state about the validation process
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final OrganisationOpponentFormData opponentFormData = (OrganisationOpponentFormData) target;
    // Validate the organisation name and type if this is not a shared organisation.
    if (!Boolean.TRUE.equals(opponentFormData.getShared())) {
      validateOrganisationName(errors, opponentFormData);
      validateRequiredField("organisationType", opponentFormData.getOrganisationType(),
          "Organisation type", errors);
    }

    validateRequiredField("relationshipToCase", opponentFormData.getRelationshipToCase(),
        "Relationship to case", errors);
    validateRequiredField("relationshipToClient", opponentFormData.getRelationshipToClient(),
        "Relationship to client", errors);

    // Validate the contact and address fields if this is not a shared organisation.
    if (!Boolean.TRUE.equals(opponentFormData.getShared())) {
      validateContactNameRole(opponentFormData.getContactNameRole(), errors);
      validateAddress(opponentFormData, errors);
      validateEmailAddress(opponentFormData.getEmailAddress(), errors);
      validateTelephoneNumber("telephoneWork", opponentFormData.getTelephoneWork(),
          false, "Telephone", errors);
      validateTelephoneNumber("faxNumber", opponentFormData.getFaxNumber(),
          false, "Fax", errors);
    }

    validateOtherInformation(opponentFormData, errors);
  }

  private void validateOtherInformation(OrganisationOpponentFormData opponentFormData,
      Errors errors) {
    if (StringUtils.hasText(opponentFormData.getOtherInformation())) {
      validateFieldFormat("otherInformation", opponentFormData.getOtherInformation(),
          STANDARD_CHARACTER_SET, "Other information", errors);
      validateFieldMaxLength("otherInformation", opponentFormData.getOtherInformation(),
          2000, "Other information", errors);
    }
  }

  private void validateOrganisationName(Errors errors,
      OrganisationOpponentFormData opponentFormData) {
    validateRequiredField("organisationName", opponentFormData.getOrganisationName(),
        "Organisation name", errors);

    if (StringUtils.hasText(opponentFormData.getOrganisationName())) {
      validateFieldFormat("organisationName", opponentFormData.getOrganisationName(),
          CHARACTER_SET_A, "Organisation name", errors);
    }
  }

  protected void validateContactNameRole(final String contactNameRole, final Errors errors) {
    if (StringUtils.hasText(contactNameRole)) {
      validateFieldFormat("contactNameRole", contactNameRole, STANDARD_CHARACTER_SET,
          "Contact name and role", errors);
    }
  }

}

