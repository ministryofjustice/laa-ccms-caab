package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;

/** Validates the organisation opponent details provided by the user. */
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
      validateOrganisationName(opponentFormData.getOrganisationName(), errors);
      validateRequiredField(
          "organisationType", opponentFormData.getOrganisationType(), "Organisation type", errors);
    }

    validateRequiredField(
        "relationshipToCase",
        opponentFormData.getRelationshipToCase(),
        "Relationship to case",
        errors);
    validateRequiredField(
        "relationshipToClient",
        opponentFormData.getRelationshipToClient(),
        "Relationship to client",
        errors);

    // Validate the contact and address fields if this is not a shared organisation.
    if (!Boolean.TRUE.equals(opponentFormData.getShared())) {
      validateContactNameRole(opponentFormData.getContactNameRole(), errors);
      validateAddress(opponentFormData, errors);
      validateEmailAddress(opponentFormData.getEmailAddress(), errors);
      validateTelephoneNumber(
          "telephoneWork", opponentFormData.getTelephoneWork(), false, "Telephone", errors);
      validateTelephoneNumber("faxNumber", opponentFormData.getFaxNumber(), false, "Fax", errors);
    }

    validateOtherInformation(opponentFormData.getOtherInformation(), errors);
  }

  /**
   * Validate the 'other information' field. The field will be restricted to the standard character
   * set. Additionally it must have no more than 2000 characters.
   *
   * @param otherInformationValue - the otherInformation field value.
   * @param errors - the errors.
   */
  private void validateOtherInformation(final String otherInformationValue, final Errors errors) {
    if (StringUtils.hasText(otherInformationValue)) {
      validateFieldFormat(
          "otherInformation",
          otherInformationValue,
          STANDARD_CHARACTER_SET,
          "Other information",
          errors);
      validateFieldMaxLength(
          "otherInformation", otherInformationValue, 2000, "Other information", errors);
    }
  }

  /**
   * Validate the 'organisation name' field. This field is mandatory, and must be resctricted to
   * character set A.
   *
   * @param organisationNameValue - the value of the organisationName field.
   * @param errors - the errors
   */
  private void validateOrganisationName(final String organisationNameValue, final Errors errors) {
    validateRequiredField("organisationName", organisationNameValue, "Organisation name", errors);

    if (StringUtils.hasText(organisationNameValue)) {
      validateFieldFormat(
          "organisationName", organisationNameValue, CHARACTER_SET_A, "Organisation name", errors);
    }
  }

  /**
   * Validate the 'contact name role' field. This field must be restricted to the standard character
   * set.
   *
   * @param contactNameRoleValue - the value of the form field.
   * @param errors - the errors.
   */
  protected void validateContactNameRole(final String contactNameRoleValue, final Errors errors) {
    if (StringUtils.hasText(contactNameRoleValue)) {
      validateFieldFormat(
          "contactNameRole",
          contactNameRoleValue,
          STANDARD_CHARACTER_SET,
          "Contact name and role",
          errors);
    }
  }
}
