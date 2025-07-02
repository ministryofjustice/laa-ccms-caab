package uk.gov.laa.ccms.caab.bean.validators.client;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SPACES_COMMAS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.DOUBLE_SPACE;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validates the correspondence address data. */
@Component
public class CorrespondenceAddressValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.AddressFormData}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return AddressFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the correspondence address data in the {@link
   * uk.gov.laa.ccms.caab.bean.AddressFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    final AddressFormData addressFormData = (AddressFormData) target;

    validateRequiredField(
        "preferredAddress", addressFormData.getPreferredAddress(), "Preferred address", errors);

    if ("CASE".equals(addressFormData.getPreferredAddress())) {
      validateRequiredField("country", addressFormData.getCountry(), "Country", errors);

      validateRequiredField(
          "houseNameNumber", addressFormData.getHouseNameNumber(), "House name / number", errors);

      validateRequiredField(
          "addressLine1", addressFormData.getAddressLine1(), "Address line 1", errors);

      validateRequiredField("cityTown", addressFormData.getCityTown(), "City / town", errors);
    }

    if (StringUtils.hasText(addressFormData.getCountry())) {
      validateRequiredField("postcode", addressFormData.getPostcode(), "Postcode", errors);

      validatePostcodeFormat(addressFormData.getCountry(), addressFormData.getPostcode(), errors);
    }

    validateAddressField(
        addressFormData.getAddressLine1(), "addressLine1", "Address line 1", errors);
    validateAddressField(
        addressFormData.getAddressLine2(), "addressLine2", "Address line 2", errors);
    validateAddressField(addressFormData.getCityTown(), "cityTown", "City /Town", errors);
    validateAddressField(addressFormData.getCounty(), "county", "County", errors);
    validateAddressField(addressFormData.getCareOf(), "careOf", "C/O", errors);
  }

  /**
   * Validates the addressLine1 in the {@link uk.gov.laa.ccms.caab.bean.AddressFormData}.
   *
   * @param value The string value to be validated.
   * @param fieldName The field name to be validated.
   * @param displayFieldName The field name to be displayed in error message.
   * @param errors The Errors object to store validation errors.
   */
  private void validateAddressField(
      final String value, final String fieldName, final String displayFieldName, Errors errors) {
    if (StringUtils.hasText(value)) {
      // check no double spaces
      if (!value.matches(ALPHA_NUMERIC_SPACES_COMMAS)) {
        errors.rejectValue(
            fieldName,
            "invalid." + fieldName,
            "Your input for '"
                + displayFieldName
                + "' contains an "
                + "invalid character. Please amend your entry using numbers, "
                + "letters and spaces only");
      } else if (value.matches(DOUBLE_SPACE)) {
        errors.rejectValue(
            fieldName,
            "invalid." + fieldName,
            "Your input for '"
                + displayFieldName
                + "'"
                + " contains double spaces. Please amend your entry.");
      }
    }
  }
}
