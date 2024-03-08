package uk.gov.laa.ccms.caab.bean.validators.application;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.EMAIL_ADDRESS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.constants.ValidationPatternConstants;

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
  public void validate(final Object target, final Errors errors) {
    final OpponentFormData opponentFormData = (OpponentFormData) target;
    validateRequiredField("relationshipToCase", opponentFormData.getRelationshipToCase(),
        "Relationship to case", errors);
    validateRequiredField("relationshipToClient", opponentFormData.getRelationshipToClient(),
        "Relationship to client", errors);
    if (StringUtils.hasText(opponentFormData.getOtherInformation())) {
      validateFieldFormat("otherInformation", opponentFormData.getOtherInformation(),
          STANDARD_CHARACTER_SET, "Other information", errors);
      validateFieldMaxLength("otherInformation", opponentFormData.getOtherInformation(),
          2000, "Other information", errors);
    }

    // Perform validation on other fields if this is not a shared organisation (ie - a new
    // organisation is being created)
    if (!opponentFormData.isShared()) {
      validateOrganisationName(errors, opponentFormData);
      validateRequiredField("organisationType", opponentFormData.getOrganisationType(),
          "Organisation type", errors);

      validateAddress(opponentFormData, errors);
      validateContactNameRole(opponentFormData.getContactNameRole(), errors);
      validateEmailAddress(opponentFormData.getEmailAddress(), errors);
      validateTelephoneNumber("telephoneWork", opponentFormData.getTelephoneWork(),
          false, "Telephone", errors);
      validateTelephoneNumber("faxNumber", opponentFormData.getFaxNumber(),
          false, "Fax", errors);
    }
  }

  private void validateOrganisationName(Errors errors, OpponentFormData opponentFormData) {
    validateRequiredField("organisationName", opponentFormData.getOrganisationName(),
        "Organisation name", errors);

    if (StringUtils.hasText(opponentFormData.getOrganisationName())) {
      validateFieldFormat("organisationName", opponentFormData.getOrganisationName(),
          CHARACTER_SET_A, "Organisation name", errors);
    }
  }

  protected void validateAddress(
      final OpponentFormData opponentFormData,
      final Errors errors) {
    if (isAddressPopulated(opponentFormData)) {
      if (StringUtils.hasText(opponentFormData.getHouseNameOrNumber())) {
        validateFieldFormat("houseNameOrNumber",
            opponentFormData.getHouseNameOrNumber(),
            STANDARD_CHARACTER_SET,
            "Building name / number",
            errors);
      }
      validateAddressField("addressLine1", opponentFormData.getAddressLine1(),
          "Address line 1", true, errors);
      validateAddressField("addressLine2", opponentFormData.getAddressLine2(),
          "Address line 2", false, errors);
      validateAddressField("city", opponentFormData.getCity(),
          "City", false, errors);
      validateAddressField("county", opponentFormData.getCounty(),
          "County", false, errors);
      validateRequiredField("country", opponentFormData.getCountry(),
          "Country", errors);
      validateInternationalPostcodeFormat(opponentFormData.getPostcode(), false, errors);
    }
  }

  protected void validateEmailAddress(final String email, final Errors errors) {
    if (StringUtils.hasText(email)) {
      validateFieldFormat("emailAddress", email, EMAIL_ADDRESS,
          "Email address", errors);
    }
  }

  protected void validateContactNameRole(final String contactNameRole, final Errors errors) {
    if (StringUtils.hasText(contactNameRole)) {
      validateFieldFormat("contactNameRole", contactNameRole, STANDARD_CHARACTER_SET,
          "Contact name and role", errors);
    }
  }

  protected void validateAddressField(final String field, final String fieldValue,
      final String displayValue, final boolean required, final Errors errors) {
    if (required) {
      validateRequiredField(field, fieldValue, displayValue, errors);
    }

    if (StringUtils.hasText(fieldValue)) {
      validateFieldFormat(field, fieldValue,
          ValidationPatternConstants.CHARACTER_SET_A, displayValue, errors);
      validateDoubleSpaces(field, fieldValue, displayValue, errors);
    }
  }

  private boolean isAddressPopulated(final OpponentFormData opponentFormData) {
    return StringUtils.hasText(opponentFormData.getHouseNameOrNumber())
        || StringUtils.hasText(opponentFormData.getAddressLine1())
        || StringUtils.hasText(opponentFormData.getAddressLine2())
        || StringUtils.hasText(opponentFormData.getCity())
        || StringUtils.hasText(opponentFormData.getCounty())
        || StringUtils.hasText(opponentFormData.getPostcode());
  }
}

