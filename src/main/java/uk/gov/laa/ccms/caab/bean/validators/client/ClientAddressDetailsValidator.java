package uk.gov.laa.ccms.caab.bean.validators.client;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SPACES_COMMAS;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.DOUBLE_SPACE;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;

/**
 * Validator component responsible for validating {@link
 * uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails} objects.
 */
@Component
public class ClientAddressDetailsValidator extends AbstractClientAddressValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataAddressDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the {@link uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientFormDataAddressDetails addressDetails = (ClientFormDataAddressDetails) target;

    if (!addressDetails.getVulnerableClient() && !addressDetails.getNoFixedAbode()) {
      validateRequiredField("country", addressDetails.getCountry(), "Country", errors);
      validateRequiredField(
          "houseNameNumber", addressDetails.getHouseNameNumber(), "House name / number", errors);
      validatePostcodeFormat(addressDetails.getCountry(), addressDetails.getPostcode(), errors);
      validateRequiredField(
          "addressLine1", addressDetails.getAddressLine1(), "Address line 1", errors);
      validateRequiredField("cityTown", addressDetails.getCityTown(), "City / Town", errors);
    } else if (addressDetails.getNoFixedAbode()) {
      validateNoFixedAbode(addressDetails, errors);
    }

    validateAddressLine1(addressDetails.getAddressLine1(), errors);
    validateAddressLine2(addressDetails.getAddressLine2(), errors);
    validateCityTown(addressDetails.getCityTown(), errors);
    validateCounty(addressDetails.getCounty(), errors);
  }

  /**
   * Validates the addressLine1 in the {@link ClientFormDataAddressDetails}.
   *
   * @param addressLine1 The string to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateAddressLine1(final String addressLine1, Errors errors) {
    if (StringUtils.hasText(addressLine1)) {
      // check no double spaces
      if (!addressLine1.matches(ALPHA_NUMERIC_SPACES_COMMAS)) {
        errors.rejectValue(
            "addressLine1",
            "invalid.addressLine1",
            "Your input for 'Address Line 1' contains an "
                + "invalid character. Please amend your entry using numbers, "
                + "letters and spaces only");
      } else if (addressLine1.matches(DOUBLE_SPACE)) {
        errors.rejectValue(
            "addressLine1",
            "invalid.addressLine1",
            "Your input for 'Address Line 1'"
                + " contains double spaces. Please amend your entry.");
      }
    }
  }

  /**
   * Validates the addressLine2 in the {@link ClientFormDataAddressDetails}.
   *
   * @param addressLine2 The string to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateAddressLine2(final String addressLine2, Errors errors) {
    if (StringUtils.hasText(addressLine2)) {
      // check no double spaces
      if (!addressLine2.matches(ALPHA_NUMERIC_SPACES_COMMAS)) {
        errors.rejectValue(
            "addressLine2",
            "invalid.addressLine2",
            "Your input for 'Address Line 2' contains an "
                + "invalid character. Please amend your entry using numbers, "
                + "letters and spaces only");
      } else if (addressLine2.matches(DOUBLE_SPACE)) {
        errors.rejectValue(
            "addressLine2",
            "invalid.addressLine2",
            "Your input for 'Address Line 2'"
                + " contains double spaces. Please amend your entry.");
      }
    }
  }

  /**
   * Validates the cityTown in the {@link ClientFormDataAddressDetails}.
   *
   * @param cityTown The string to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateCityTown(final String cityTown, Errors errors) {
    if (StringUtils.hasText(cityTown)) {
      // check no double spaces
      if (!cityTown.matches(ALPHA_NUMERIC_SPACES_COMMAS)) {
        errors.rejectValue(
            "cityTown",
            "invalid.cityTown",
            "Your input for 'City /Town' contains an "
                + "invalid character. Please amend your entry using numbers, "
                + "letters and spaces only");
      } else if (cityTown.matches(DOUBLE_SPACE)) {
        errors.rejectValue(
            "cityTown",
            "invalid.cityTown",
            "Your input for 'City /Town'" + " contains double spaces. Please amend your entry.");
      }
    }
  }

  /**
   * Validates the county in the {@link ClientFormDataAddressDetails}.
   *
   * @param county The string to be validated.
   * @param errors The Errors object to store validation errors.
   */
  private void validateCounty(final String county, Errors errors) {
    if (StringUtils.hasText(county)) {
      // check no double spaces
      if (!county.matches(ALPHA_NUMERIC_SPACES_COMMAS)) {
        errors.rejectValue(
            "county",
            "invalid.county",
            "Your input for 'County' contains an "
                + "invalid character. Please amend your entry using numbers, "
                + "letters and spaces only");
      } else if (county.matches(DOUBLE_SPACE)) {
        errors.rejectValue(
            "county",
            "invalid.county",
            "Your input for 'County'" + " contains double spaces. Please amend your entry.");
      }
    }
  }
}
