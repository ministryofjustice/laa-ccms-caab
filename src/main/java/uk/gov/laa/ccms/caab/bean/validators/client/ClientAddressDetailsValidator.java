package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails} objects.
 */
@Component
public class ClientAddressDetailsValidator extends AbstractClientAddressValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails},
   *         {@code false} otherwise.
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
      validateRequiredField("country", addressDetails.getCountry(),
          "Country", errors);
      validateRequiredField("houseNameNumber", addressDetails.getHouseNameNumber(),
          "House name / number", errors);
      validatePostcodeFormat(addressDetails.getCountry(), addressDetails.getPostcode(), errors);
      validateRequiredField("addressLine1", addressDetails.getAddressLine1(),
          "Address line 1", errors);
      validateRequiredField("cityTown", addressDetails.getCityTown(),
          "City / Town", errors);
    } else if (addressDetails.getNoFixedAbode()) {
      validateNoFixedAbode(addressDetails, errors);
    }
  }
}
