package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.util.UkPostcode;

/**
 * Validator component responsible for validating {@link
 * uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails} objects.
 */
@Component
public class FindAddressValidator extends AbstractClientAddressValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails}, {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ClientFormDataAddressDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the {@link uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {

    String country = null;
    String postcode = "";

    if (target instanceof ClientFormDataAddressDetails addressDetails) {

      if (addressDetails.getNoFixedAbode()) {
        validateNoFixedAbode(addressDetails, errors);
      }

      country = addressDetails.getCountry();
      postcode = addressDetails.getPostcode();

    } else if (target instanceof AddressFormData addressDetails) {

      country = addressDetails.getCountry();
      postcode = addressDetails.getPostcode();
    }

    if (StringUtils.hasText(country) && !"GBR".equals(country)) {
      errors.rejectValue(
          "country",
          "required.GBR",
          "The address lookup system is not available for the country you have "
              + "selected. Please enter the address manually.");
    }

    if (!StringUtils.hasText(postcode)) {
      validateRequiredField("postcode", postcode, "Postcode", errors);
    } else if (!UkPostcode.isValid(postcode)) {
      errors.rejectValue("postcode", "invalid.postcode", "Enter a full UK postcode");
    }
  }
}
