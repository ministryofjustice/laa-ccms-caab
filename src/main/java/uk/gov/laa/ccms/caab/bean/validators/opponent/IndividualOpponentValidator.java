package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SLASH_SPACE_STRING;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_C;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_E;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.NATIONAL_INSURANCE_NUMBER_PATTERN;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.OpponentFormData;

/**
 * Validates the individual opponent details provided by the user.
 */
@Component
public class IndividualOpponentValidator extends AbstractOpponentValidator {

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

    validateRequiredField("title", opponentFormData.getTitle(),
        "Title", errors);
    validateNameComponent("surname", opponentFormData.getSurname(), "Surname",
        true, CHARACTER_SET_E, errors);
    validateNameComponent("firstName", opponentFormData.getFirstName(),
        "First name", true, CHARACTER_SET_C, errors);
    validateNameComponent("middleNames", opponentFormData.getMiddleNames(),
        "Middle name(s)", false, CHARACTER_SET_C, errors);

    validateDateComponent("dobDay", opponentFormData.getDobDay(),
        "Date of birth (day)", opponentFormData.isDateOfBirthMandatory(), errors);
    validateDateComponent("dobMonth", opponentFormData.getDobMonth(),
        "Date of birth (month)", opponentFormData.isDateOfBirthMandatory(), errors);
    validateDateComponent("dobYear", opponentFormData.getDobYear(),
        "Date of birth (year)", opponentFormData.isDateOfBirthMandatory(), errors);

    validateRequiredField("relationshipToCase", opponentFormData.getRelationshipToCase(),
        "Relationship to case", errors);
    validateRequiredField("relationshipToClient", opponentFormData.getRelationshipToClient(),
        "Relationship to client", errors);

    if (StringUtils.hasText(opponentFormData.getNationalInsuranceNumber())) {
      validateFieldFormat("nationalInsuranceNumber",
          opponentFormData.getNationalInsuranceNumber(),
          NATIONAL_INSURANCE_NUMBER_PATTERN,
          "National insurance number",
          errors);
    }

    validateAddress(opponentFormData, errors);

    validateEmailAddress(opponentFormData.getEmailAddress(), errors);

    validateTelephoneNumber("telephoneHome", opponentFormData.getTelephoneHome(),
        false, "Telephone - home", errors);
    validateTelephoneNumber("telephoneWork", opponentFormData.getTelephoneWork(),
        false, "Telephone - work", errors);
    validateTelephoneNumber("telephoneMobile", opponentFormData.getTelephoneMobile(),
        false, "Telephone - mobile", errors);

    validateTelephoneNumber("faxNumber", opponentFormData.getFaxNumber(),
        false, "Fax", errors);

    if (StringUtils.hasText(opponentFormData.getCertificateNumber())) {
      validateFieldFormat("certificateNumber",
          opponentFormData.getCertificateNumber(),
          ALPHA_NUMERIC_SLASH_SPACE_STRING,
          "Certificate number",
          errors);
    }
  }

  private void validateNameComponent(final String fieldId,
      final String fieldValue,
      final String displayValue,
      final boolean required,
      final String characterSetRestriction,
      Errors errors) {
    if (required) {
      validateRequiredField(fieldId, fieldValue, displayValue, errors);
    }

    validateFieldFormat(fieldId, fieldValue, characterSetRestriction, displayValue, errors);
    validateFirstCharAlpha(fieldId, fieldValue, displayValue, errors);
    validateDoubleSpaces(fieldId, fieldValue, displayValue, errors);
  }

  private void validateDateComponent(final String fieldId, final String fieldValue,
      final String displayValue, boolean required, Errors errors) {
    if (required) {
      validateRequiredField(fieldId, fieldValue, displayValue, errors);
    }

    if (StringUtils.hasText(fieldValue)) {
      validateNumericField(fieldId, fieldValue, displayValue, errors);
    }
  }


}

