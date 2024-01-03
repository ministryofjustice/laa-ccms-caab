package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validates the correspondence address data.
 */
@Component
public class CorrespondenceAddressValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.AddressFormData},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return AddressFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the correspondence address data in the
   * {@link uk.gov.laa.ccms.caab.bean.AddressFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    final AddressFormData addressFormData = (AddressFormData) target;

    validateRequiredField("preferredAddress", addressFormData.getPreferredAddress(),
        "Preferred address", errors);

    if (StringUtils.hasText(addressFormData.getCountry())) {
      validateRequiredField("postcode", addressFormData.getPostcode(),
          "Postcode", errors);
    }
  }
}
