package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static uk.gov.laa.ccms.caab.constants.CharacterLimitationConstants.OTHER_INFORMATION_CHARACTER_SIZE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.EMAIL_ADDRESS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.constants.ValidationPatternConstants;

/** Abstract Opponent validator to group common validation methods. */
public abstract class AbstractOpponentValidator extends AbstractValidator {

  private static final String OTHER_INFORMATION_DISPLAY_NAME = "Other information";

  /**
   * Validate the 'other information' field, which both opponent types carry on {@link
   * AbstractOpponentFormData}. Restricted to the standard character set and 2000 characters.
   *
   * <p>Lives here rather than on one subclass because it previously did: the organisation opponent
   * checked this field and the individual opponent did not, leaving the same field on the same bean
   * validated on one screen and unrestricted on the other.
   *
   * @param otherInformationValue - the otherInformation field value.
   * @param errors - the errors.
   */
  protected void validateOtherInformation(final String otherInformationValue, final Errors errors) {
    if (StringUtils.hasText(otherInformationValue)) {
      validateFieldFormat(
          "otherInformation",
          otherInformationValue,
          STANDARD_CHARACTER_SET,
          OTHER_INFORMATION_DISPLAY_NAME,
          errors);
      validateFieldMaxLength(
          "otherInformation",
          otherInformationValue,
          OTHER_INFORMATION_CHARACTER_SIZE,
          OTHER_INFORMATION_DISPLAY_NAME,
          errors);
    }
  }

  protected void validateAddress(
      final AbstractOpponentFormData opponentFormData, final Errors errors) {
    if (isAddressPopulated(opponentFormData)) {
      if (StringUtils.hasText(opponentFormData.getHouseNameOrNumber())) {
        validateFieldFormat(
            "houseNameOrNumber",
            opponentFormData.getHouseNameOrNumber(),
            STANDARD_CHARACTER_SET,
            "Building name / number",
            errors);
      }
      validateAddressField(
          "addressLine1", opponentFormData.getAddressLine1(), "Address line 1", true, errors);
      validateAddressField(
          "addressLine2", opponentFormData.getAddressLine2(), "Address line 2", false, errors);
      validateAddressField("city", opponentFormData.getCity(), "City", false, errors);
      validateAddressField("county", opponentFormData.getCounty(), "County", false, errors);
      validateRequiredField("country", opponentFormData.getCountry(), "Country", errors);

      validatePostcodeFormat(opponentFormData.getCountry(), opponentFormData.getPostcode(), errors);
    }
  }

  protected void validateEmailAddress(final String email, final Errors errors) {
    if (StringUtils.hasText(email)) {
      validateFieldFormat("emailAddress", email, EMAIL_ADDRESS, "Email address", errors);
    }
  }

  protected void validateAddressField(
      final String field,
      final String fieldValue,
      final String displayValue,
      final boolean required,
      final Errors errors) {
    if (required) {
      validateRequiredField(field, fieldValue, displayValue, errors);
    }

    if (StringUtils.hasText(fieldValue)) {
      validateFieldFormat(
          field, fieldValue, ValidationPatternConstants.CHARACTER_SET_A, displayValue, errors);
      validateDoubleSpaces(field, fieldValue, displayValue, errors);
    }
  }

  boolean isAddressPopulated(final AbstractOpponentFormData opponentFormData) {
    return StringUtils.hasText(opponentFormData.getHouseNameOrNumber())
        || StringUtils.hasText(opponentFormData.getAddressLine1())
        || StringUtils.hasText(opponentFormData.getAddressLine2())
        || StringUtils.hasText(opponentFormData.getCity())
        || StringUtils.hasText(opponentFormData.getCounty())
        || StringUtils.hasText(opponentFormData.getPostcode());
  }
}
